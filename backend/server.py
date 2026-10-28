import base64
import json
import logging
import os
import re
import sys
import time
import uuid
import zipfile
import requests
from io import BytesIO
from PIL import Image

Image.MAX_IMAGE_PIXELS = 25_000_000

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("AuraMirror")

app = FastAPI(title="AuraMirror Skin AI MVP")

# Security: Restrict CORS strictly to local origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://127.0.0.1:8000", "http://localhost:8000"],
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.path.join(BASE_DIR, "static")
SECRETS_PATH = os.environ.get("YOUCAM_SECRETS_PATH", os.path.join(BASE_DIR, "secrets", "youcam_api.json"))

CACHE_DIR = os.path.join(BASE_DIR, "cache")
os.makedirs(CACHE_DIR, exist_ok=True)
os.makedirs(STATIC_DIR, exist_ok=True)

app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

def get_youcam_api_key():
    env_key = os.environ.get("YOUCAM_API_KEY", "").strip()
    if env_key:
        return env_key
    if os.path.exists(SECRETS_PATH):
        try:
            with open(SECRETS_PATH, "r", encoding="utf-8") as f:
                key = json.load(f).get("api_key", "").strip()
                if key:
                    return key
        except Exception as e:
            logger.warning(f"Failed to read secrets file: {e}")
    return ""

def format_metric(val):
    if val is None or val == "":
        return "N/A"
    return str(val)

# Rule-Based Dermatological Ingredient Suggestions Table (Honest & Non-hallucinatory)
RULE_BASED_INGREDIENTS = {
    "oiliness": {
        "tag": "T-Zone Sebum & Pores",
        "product_name": "Skin Perfecting 2% BHA Liquid Exfoliant",
        "brand": "Paula's Choice",
        "active_ingredients": "Salicylic Acid (BHA 2%), Green Tea Extract",
        "purpose": "Clears clogged T-zone pores and balances sebum levels",
        "buy_url": "https://www.google.com/search?q=Paulas+Choice+2+BHA+Liquid+Exfoliant"
    },
    "dark_circle": {
        "tag": "Eye Fatigue & Dark Circles",
        "product_name": "Caffeine Solution 5% + EGCG",
        "brand": "The Ordinary",
        "active_ingredients": "Caffeine (5%), Epigallocatechin Gallatyl Glucoside (EGCG)",
        "purpose": "Reduces the appearance of eye contour puffiness and dark circles",
        "buy_url": "https://www.google.com/search?q=The+Ordinary+Caffeine+Solution+5+EGCG"
    },
    "moisture": {
        "tag": "Barrier & Deep Hydration",
        "product_name": "DIVE-IN Low Molecular Hyaluronic Acid Serum",
        "brand": "Torriden",
        "active_ingredients": "5D Complex Hyaluronic Acid, D-Panthenol, Allantoin",
        "purpose": "Provides instant deep hydration and replenishes the skin moisture barrier",
        "buy_url": "https://www.google.com/search?q=Torriden+DIVE+IN+Hyaluronic+Acid+Serum"
    },
    "redness": {
        "tag": "Calming & Barrier Relief",
        "product_name": "Madagascar Centella Ampoule",
        "brand": "SKIN1004",
        "active_ingredients": "Centella Asiatica Extract (100%), Madecassic Acid",
        "purpose": "Soothes irritated skin and calms facial redness",
        "buy_url": "https://www.google.com/search?q=SKIN1004+Madagascar+Centella+Ampoule"
    },
    "pore": {
        "tag": "Pore Refinement",
        "product_name": "Niacinamide 10% + Zinc 1%",
        "brand": "The Ordinary",
        "active_ingredients": "Niacinamide (Vitamin B3 10%), Zinc PCA (1%)",
        "purpose": "Tightens enlarged pores and refines irregular skin texture",
        "buy_url": "https://www.google.com/search?q=The+Ordinary+Niacinamide+10+Zinc+1"
    }
}

class AnalyzeRequest(BaseModel):
    image_base64: str | None = None

class PrescribeRequest(BaseModel):
    skin_metrics: dict
    wearable_data: dict

class ProductCheckRequest(BaseModel):
    product_name: str = ""
    ingredients_text: str
    skin_metrics: dict

@app.get("/")
def read_root():
    index_file = os.path.join(STATIC_DIR, "index.html")
    if os.path.exists(index_file):
        return FileResponse(index_file)
    return {"message": "AuraMirror AI API Server is running. Static index.html not yet found."}

@app.post("/api/analyze")
def analyze_skin(req: AnalyzeRequest):
    if not req.image_base64 or not req.image_base64.strip():
        raise HTTPException(status_code=400, detail="Webcam facial frame is missing. Please face the camera and scan.")

    if len(req.image_base64) > 12_000_000:
        raise HTTPException(status_code=413, detail="Encoded image payload exceeds maximum allowed size.")

    api_key = get_youcam_api_key()
    if not api_key:
        raise HTTPException(status_code=500, detail="YouCam API key is not configured on the server.")

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    base_url = "https://yce-api-01.makeupar.com"
    deadline = time.monotonic() + 75.0

    def remaining_timeout(step_cap: float) -> float:
        rem = deadline - time.monotonic()
        if rem <= 0.5:
            raise HTTPException(status_code=504, detail="Skin analysis overall time budget (75s) exceeded.")
        return min(step_cap, rem)

    try:
        # 1. Prepare Image strictly in-memory (Zero temporary disk files)
        try:
            data = req.image_base64
            if "," in data:
                data = data.split(",", 1)[1]
            img_bytes = base64.b64decode(data, validate=False)
            if len(img_bytes) > 10_000_000:
                raise ValueError("Decoded image exceeds 10MB limit.")
            with Image.open(BytesIO(img_bytes)) as opened:
                w, h = opened.size
                if w <= 0 or h <= 0 or w > 8192 or h > 8192 or (w * h) > 25_000_000:
                    raise ValueError("Image dimensions are out of safe bounds.")
                im = opened.convert("RGB")
            min_dim = min(w, h)
            left = (w - min_dim) // 2
            top = (h - min_dim) // 2
            cropped = im.crop((left, top, left + min_dim, top + min_dim))
            final_im = cropped.resize((600, 600), Image.LANCZOS)
            jpeg_buf = BytesIO()
            final_im.save(jpeg_buf, format="JPEG", quality=95)
            jpeg_payload = jpeg_buf.getvalue()
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Image preprocessing failed: {str(e)}")

        file_size = len(jpeg_payload)
        file_name = f"input_{uuid.uuid4().hex}.jpg"

        # 2. YouCam File Upload Slot
        slot_http = requests.post(
            f"{base_url}/s2s/v2.0/file",
            headers=headers,
            json={"files": [{"content_type": "image/jpeg", "file_name": file_name, "file_size": file_size}]},
            timeout=remaining_timeout(15.0)
        )
        if not slot_http.ok:
            raise HTTPException(status_code=502, detail=f"YouCam file slot error (HTTP {slot_http.status_code})")
        slot_resp = slot_http.json()

        files_list = slot_resp.get("data", {}).get("files", [])
        if not files_list:
            raise HTTPException(status_code=502, detail="YouCam file slot response missing file descriptor")
        file_obj = files_list[0]
        file_id = file_obj["file_id"]
        req_obj = file_obj["requests"][0]
        upload_url = req_obj["url"]
        upload_headers = req_obj.get("headers", {"Content-Type": "image/jpeg"})

        put_resp = requests.put(
            upload_url,
            data=jpeg_payload,
            headers=upload_headers,
            timeout=remaining_timeout(20.0)
        )
        if put_resp.status_code not in [200, 201]:
            raise HTTPException(status_code=502, detail=f"S3 upload failed (HTTP {put_resp.status_code})")

        # 3. Create Skin Analysis Task
        actions = ["redness", "oiliness", "pore", "acne", "moisture", "texture", "wrinkle", "age_spot", "dark_circle_v2"]
        task_http = requests.post(
            f"{base_url}/s2s/v2.0/task/skin-analysis",
            headers=headers,
            json={"src_file_id": file_id, "dst_actions": actions},
            timeout=remaining_timeout(15.0)
        )
        if not task_http.ok:
            raise HTTPException(status_code=502, detail=f"YouCam task creation failed (HTTP {task_http.status_code})")
        task_resp = task_http.json()

        task_id = task_resp.get("data", {}).get("task_id")
        if not task_id:
            raise HTTPException(status_code=400, detail="YouCam task creation returned no task_id")

        # 4. Synchronous Poll Result within overall deadline
        zip_url = None
        for _ in range(25):
            rem = deadline - time.monotonic()
            if rem <= 1.0:
                break
            time.sleep(min(1.5, max(0.1, rem - 0.5)))
            poll_http = requests.get(
                f"{base_url}/s2s/v2.0/task/skin-analysis/{task_id}",
                headers=headers,
                timeout=remaining_timeout(10.0)
            )
            if not poll_http.ok:
                raise HTTPException(status_code=502, detail=f"YouCam polling failed (HTTP {poll_http.status_code})")
            poll = poll_http.json()
            st = poll.get("data", {}).get("task_status")
            if st == "success":
                zip_url = poll.get("data", {}).get("results", {}).get("url")
                break
            elif st == "error":
                err_msg = poll.get("data", {}).get("error_message", "Unknown skin analysis error")
                raise HTTPException(status_code=400, detail=err_msg)

        if not zip_url:
            raise HTTPException(status_code=504, detail="Skin analysis polling timed out")

        # 5. Streamed In-Memory ZIP Download & Bounded Decompression (Pre-allocation limits)
        with requests.get(zip_url, stream=True, timeout=remaining_timeout(20.0)) as zip_http:
            if not zip_http.ok:
                raise HTTPException(status_code=502, detail=f"Result archive download failed (HTTP {zip_http.status_code})")
            zip_buf = BytesIO()
            downloaded = 0
            for chunk in zip_http.iter_content(chunk_size=65536):
                if time.monotonic() >= deadline:
                    raise HTTPException(status_code=504, detail="Result archive download exceeded time budget.")
                if chunk:
                    downloaded += len(chunk)
                    if downloaded > 20_000_000:
                        raise HTTPException(status_code=502, detail="Result archive exceeds 20MB stream limit.")
                    zip_buf.write(chunk)
            zip_bytes = zip_buf.getvalue()

        score_data = {}
        masks = {}
        total_uncompressed = 0

        with zipfile.ZipFile(BytesIO(zip_bytes), "r") as zf:
            infos = zf.infolist()
            if len(infos) > 32:
                raise HTTPException(status_code=502, detail="Result archive contains too many entries.")

            # Pass 1: Always extract score_info.json first so optional mask size caps never block scores
            for info in infos:
                if time.monotonic() >= deadline:
                    raise HTTPException(status_code=504, detail="Result archive processing exceeded time budget.")
                base = os.path.basename(info.filename)
                if base == "score_info.json" and 0 <= info.file_size <= 500_000:
                    try:
                        raw_json = zf.read(info)
                        if len(raw_json) <= 500_000:
                            parsed_json = json.loads(raw_json.decode("utf-8"))
                            if isinstance(parsed_json, dict):
                                score_data = parsed_json
                    except Exception as json_err:
                        logger.warning(f"Failed to parse score_info.json: {json_err}")
                    break

            if not score_data:
                raise HTTPException(status_code=502, detail="Skin analysis result archive missing valid score_info.json")

            # Pass 2: Extract supported PNG masks within the 10MB cumulative budget (isolated per entry)
            for info in infos:
                if time.monotonic() >= deadline:
                    logger.warning("Mask extraction stopped early due to deadline; returning valid score_info.")
                    break
                base = os.path.basename(info.filename)
                if not base.endswith("_output.png") or base in masks or len(masks) >= 12:
                    continue
                if info.file_size <= 0 or info.file_size > 3_500_000:
                    continue
                if total_uncompressed + info.file_size > 10_000_000:
                    break
                try:
                    png_bytes = zf.read(info)
                    if len(png_bytes) > 3_500_000:
                        continue
                    with Image.open(BytesIO(png_bytes)) as mask_im:
                        mw, mh = mask_im.size
                        if mw <= 0 or mh <= 0 or mw > 2048 or mh > 2048 or (mw * mh) > 4_000_000:
                            continue
                    total_uncompressed += len(png_bytes)
                    masks[base] = f"data:image/png;base64,{base64.b64encode(png_bytes).decode('utf-8')}"
                except Exception as mask_err:
                    logger.warning(f"Skipping corrupted mask {base}: {mask_err}")
                    continue

        return {
            "status": "success",
            "task_id": task_id,
            "score_info": score_data,
            "masks": masks
        }

    except HTTPException:
        raise
    except requests.Timeout:
        raise HTTPException(status_code=504, detail="Upstream skin analysis request timed out.")
    except Exception as e:
        logger.error(f"Analysis error: {e}")
        raise HTTPException(status_code=500, detail="Skin analysis failed due to an internal processing error.")

@app.post("/api/prescribe")
def prescribe_routine(req: PrescribeRequest):
    metrics = req.skin_metrics or {}
    wearable = req.wearable_data or {}

    raw_sleep = wearable.get("sleep_hours")
    sleep_h = None
    if isinstance(raw_sleep, (int, float)) and not isinstance(raw_sleep, bool) and 0.0 <= float(raw_sleep) <= 24.0:
        sleep_h = round(float(raw_sleep), 1)

    sleep_ctx = f"{sleep_h}h sleep session context" if sleep_h is not None else "sleep context unavailable"

    # Strict Validation: Normalize to integer scores (0 <= score <= 100) so threshold comparison and output never contradict
    thresholds = {
        "oiliness": 75,
        "dark_circle": 80,
        "moisture": 75,
        "redness": 80,
        "pore": 75
    }
    order_index = {k: idx for idx, k in enumerate(thresholds.keys())}
    normalized_scores = {}
    for k in thresholds:
        v = metrics.get(k)
        if isinstance(v, (int, float)) and not isinstance(v, bool) and 0 <= float(v) <= 100:
            normalized_scores[k] = int(round(float(v)))

    if not normalized_scores:
        raise HTTPException(
            status_code=400,
            detail="No evaluable numerical skin scores found in supported categories. Please scan face first."
        )

    # Sort concerns identically to Android UI: (score, score - threshold, canonical_order)
    concern_pairs = [
        (k, score)
        for k, score in normalized_scores.items()
        if score < thresholds[k]
    ]
    concern_pairs.sort(key=lambda item: (item[1], item[1] - thresholds[item[0]], order_index[item[0]]))
    concerns = [k for k, _ in concern_pairs]

    if not concerns:
        # For optimal_balance, prefer gentle hydration/calming barrier options rather than strong exfoliants
        gentle_keys = [k for k in ("moisture", "redness", "dark_circle") if k in normalized_scores]
        if not gentle_keys:
            gentle_keys = list(normalized_scores.keys())
        gentle_sorted = sorted(
            gentle_keys,
            key=lambda k: (normalized_scores[k], normalized_scores[k] - thresholds[k], order_index[k])
        )
        maintenance_recs = []
        for k in gentle_sorted[:2]:
            if k in RULE_BASED_INGREDIENTS:
                rec = dict(RULE_BASED_INGREDIENTS[k])
                rec["metric_key"] = k
                rec["score"] = normalized_scores[k]
                rec["threshold"] = thresholds[k]
                maintenance_recs.append(rec)
        return {
            "source": "rule_based_ingredient_engine",
            "assessment_status": "optimal_balance",
            "disclaimer": "This analysis is for general skincare wellness education and is not a medical diagnosis.",
            "headline": f"Evaluated scores ({len(normalized_scores)}/{len(thresholds)}) meet prototype thresholds ({sleep_ctx})",
            "root_causes": [
                f"Evaluated {len(normalized_scores)} of {len(thresholds)} rule-supported categories; missing categories were not assessed.",
                f"Sleep context ({sleep_ctx}) shown for non-medical wellness reference only."
            ],
            "routine": {
                "morning": "Maintain your gentle cleansing routine and apply daily broad-spectrum sun protection.",
                "evening": "Apply light restorative nighttime moisturizer and get adequate rest.",
                "lifestyle": "Stay consistently hydrated throughout the day and maintain regular sleep hygiene."
            },
            "recommendations": maintenance_recs
        }

    selected_recommendations = []
    for c in concerns[:3]:
        if c in RULE_BASED_INGREDIENTS:
            rec = dict(RULE_BASED_INGREDIENTS[c])
            rec["metric_key"] = c
            rec["score"] = normalized_scores[c]
            rec["threshold"] = thresholds[c]
            selected_recommendations.append(rec)

    return {
        "source": "rule_based_ingredient_engine",
        "assessment_status": "targeted_concerns",
        "disclaimer": "This analysis is for general skincare wellness education and is not a medical diagnosis.",
        "headline": f"Targeted skincare guidance for identified scores ({sleep_ctx})",
        "root_causes": [
            "Observed visual metrics indicate targeted areas for topical barrier support.",
            f"Sleep context ({sleep_ctx}) shown for non-medical wellness reference only."
        ],
        "routine": {
            "morning": "Cleanse gently with a mild cleanser, apply targeted formula to areas of concern, and finish with broad-spectrum sunscreen.",
            "evening": "Wash thoroughly, replenish the skin moisture barrier with a light nighttime moisturizer, and rest.",
            "lifestyle": "Prioritize steady daily hydration and consistent recovery sleep."
        },
        "recommendations": selected_recommendations
    }

@app.post("/api/check-product")
def check_product_compatibility(req: ProductCheckRequest):
    raw_text = (req.ingredients_text or "").strip()
    if not raw_text:
        raise HTTPException(status_code=400, detail="Please enter or select cosmetic ingredients to check.")
    if len(raw_text) > 5000:
        raise HTTPException(status_code=413, detail="Ingredients text is too long.")

    # Tokenize by comma/semicolon/newline and match on word boundaries to avoid substring false positives (e.g., polyglycerin != glycerin)
    raw_tokens = [t.strip() for t in re.split(r"[,;\n\r]+", raw_text) if t.strip()]
    if not raw_tokens:
        raw_tokens = [raw_text]

    def find_matching_tokens(keywords: list[str]) -> list[str]:
        matched_list = []
        seen_lower = set()
        for tok in raw_tokens:
            tok_lower = tok.lower()
            # Also strip parenthetical aliases e.g. "Cocos Nucifera (Coconut) Oil" -> "cocos nucifera oil"
            tok_de_paren = re.sub(r"\s*\([^)]*\)\s*", " ", tok_lower)
            tok_de_paren = re.sub(r"\s+", " ", tok_de_paren).strip()
            paren_parts = [p.strip() for p in re.findall(r"\(([^)]+)\)", tok_lower) if p.strip()]
            candidates = [tok_lower, tok_de_paren] + paren_parts

            matched_kw = None
            for kw in keywords:
                kw_lower = kw.lower().strip()
                pattern = r"(?<![a-z0-9\-])" + re.escape(kw_lower) + r"(?![a-z0-9\-])"
                if any(re.search(pattern, cand) for cand in candidates):
                    matched_kw = kw_lower
                    break
            if matched_kw is not None and tok_lower not in seen_lower:
                seen_lower.add(tok_lower)
                clean_tok = tok.strip()
                if len(clean_tok) > 55:
                    if matched_kw not in clean_tok[:55].lower():
                        clean_tok = f"{clean_tok[:40]}... [{matched_kw}]"
                    else:
                        clean_tok = clean_tok[:55] + "..."
                matched_list.append(clean_tok)
        return matched_list

    metrics = req.skin_metrics or {}

    def get_optional_score(key: str) -> int | None:
        val = metrics.get(key)
        if isinstance(val, (int, float)) and not isinstance(val, bool) and 0 <= float(val) <= 100:
            return int(round(float(val)))
        return None

    scores_map = {
        "redness": get_optional_score("redness"),
        "oiliness": get_optional_score("oiliness"),
        "moisture": get_optional_score("moisture"),
        "pore": get_optional_score("pore"),
        "dark_circle": get_optional_score("dark_circle") if get_optional_score("dark_circle") is not None else get_optional_score("dark_circle_v2")
    }
    evaluated_keys = [k for k, v in scores_map.items() if v is not None]
    missing_keys = [k for k, v in scores_map.items() if v is None]

    if not evaluated_keys:
        raise HTTPException(status_code=400, detail="No valid skin scores available to compare against ingredients.")

    # Rule-based heuristic dictionary (strictly lowercase keywords, tied only to actually measured scores)
    CONFLICT_RULES = [
        (["alcohol denat", "denatured alcohol", "sd alcohol", "ethanol"], "redness", 80,
         "Drying Alcohol", "May irritate redness-prone skin"),
        (["alcohol denat", "denatured alcohol", "sd alcohol", "ethanol"], "moisture", 75,
         "Drying Alcohol", "May dry out the moisture barrier"),
        (["fragrance", "parfum", "essential oil", "limonene", "linalool"], "redness", 80,
         "Fragrance / Essential Oil", "Potential sensitizer when redness is below threshold"),
        (["menthol", "peppermint", "camphor", "eucalyptus"], "redness", 80,
         "Cooling Agent", "Can trigger flushing on redness-prone skin"),
        (["coconut oil", "cocos nucifera oil", "isopropyl myristate", "shea butter", "lanolin"], "oiliness", 75,
         "Heavy Lipid / Ester", "May feel heavy on sebum-prone skin"),
        (["coconut oil", "cocos nucifera oil", "isopropyl myristate", "isopropyl palmitate"], "pore", 75,
         "Comedogenic Ester / Oil", "May congest pore-prone areas"),
        (["glycolic acid", "lactic acid", "retinol", "retinal", "tretinoin"], "redness", 75,
         "Potent Acid / Retinoid", "Strong active; patch test when redness score is low")
    ]

    BENEFIT_RULES = [
        (["centella", "centella asiatica", "madecassoside", "asiaticoside", "cica", "allantoin", "bisabolol"], "redness", 80,
         "Soothing Complex", "Supports calming & barrier comfort"),
        (["hyaluronic acid", "sodium hyaluronate", "glycerin", "panthenol", "ceramide", "ceramide np", "squalane", "beta-glucan"], "moisture", 75,
         "Humectant / Ceramide", "Supports hydration & moisture barrier"),
        (["salicylic acid", "bha", "betaine salicylate", "zinc pca", "green tea", "camellia sinensis"], "oiliness", 75,
         "Sebum-Balancing Active", "Helps balance surface oiliness"),
        (["niacinamide", "pha", "gluconolactone", "kaolin"], "pore", 75,
         "Pore-Refining Active", "Helps refine pore appearance & texture"),
        (["caffeine", "egcg", "peptide", "ascorbic acid", "vitamin c"], "dark_circle", 80,
         "Eye-Contour Active", "Supports under-eye tone & vitality")
    ]

    conflicts = []
    cautions = []
    for keywords, metric_key, threshold_val, category_label, note in CONFLICT_RULES:
        matched_tokens = find_matching_tokens(keywords)
        if not matched_tokens:
            continue
        score_val = scores_map.get(metric_key)
        for matched in matched_tokens:
            display_ing = f"{matched} ({category_label})"
            if score_val is None:
                cautions.append({
                    "ingredient": display_ing,
                    "reason": f"{note} — {metric_key} was not measured in this scan, so compatibility cannot be confirmed; patch test advised.",
                    "metric": metric_key
                })
            elif score_val < threshold_val:
                conflicts.append({
                    "ingredient": display_ing,
                    "reason": f"{note} (measured {metric_key}: {score_val}/100 < threshold {threshold_val})",
                    "metric": metric_key
                })
            else:
                cautions.append({
                    "ingredient": display_ing,
                    "reason": f"Detected in formula; your measured {metric_key} ({score_val}/100) currently meets threshold ({threshold_val}), but patch testing is still advised.",
                    "metric": metric_key
                })

    benefits = []
    for keywords, metric_key, threshold_val, category_label, note in BENEFIT_RULES:
        score_val = scores_map.get(metric_key)
        if score_val is None:
            continue
        matched_tokens = find_matching_tokens(keywords)
        if not matched_tokens:
            continue
        for matched in matched_tokens:
            display_ing = f"{matched} ({category_label})"
            target_status = f"targets below-threshold {metric_key} ({score_val} < {threshold_val})" if score_val < threshold_val else f"supports {metric_key} maintenance ({score_val}/100)"
            benefits.append({
                "ingredient": display_ing,
                "reason": f"{note} — {target_status}",
                "metric": metric_key
            })

    if conflicts:
        verdict = "CONFLICT"
        fit_score = max(15, 60 - len(conflicts) * 15)
        summary = f"Heuristic check flagged {len(conflicts)} potential conflict(s) against your below-threshold scores."
    elif cautions and benefits:
        verdict = "MIXED_CAUTION"
        fit_score = 70
        summary = f"Contains {len(benefits)} supportive match(es) alongside {len(cautions)} caution note(s). Review before use."
    elif cautions:
        verdict = "CAUTION"
        fit_score = 65
        summary = f"Detected {len(cautions)} potentially sensitizing or unassessed trigger ingredient(s); patch test advised."
    elif benefits:
        verdict = "GOOD_MATCH"
        fit_score = min(95, 80 + len(benefits) * 5)
        summary = f"Matched {len(benefits)} supportive active item(s) with no conflict keywords detected across measured metrics ({', '.join(evaluated_keys)})."
    else:
        verdict = "NEUTRAL"
        fit_score = 50
        summary = "No rule-table conflict or benefit keywords matched in the provided INCI text (unmatched ingredients are not evaluated)."

    if missing_keys:
        summary += f" (Note: {', '.join(missing_keys)} not measured in this scan.)"

    return {
        "source": "rule_based_inci_matcher",
        "product_name": req.product_name.strip() or "Custom INCI Formula",
        "verdict": verdict,
        "fit_score": fit_score,
        "summary": summary,
        "conflicts": conflicts,
        "cautions": cautions,
        "benefits": benefits
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)
