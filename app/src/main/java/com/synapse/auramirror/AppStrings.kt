package com.synapse.auramirror

enum class AppLang(val code: String, val buttonLabel: String) {
    EN("EN", "EN"),
    JA("JA", "日本語"),
    ZH("ZH", "中文"),
    KO("KO", "한국어");

    fun next(): AppLang = when (this) {
        EN -> JA
        JA -> ZH
        ZH -> KO
        KO -> EN
    }
}

object AppI18n {

    fun metricLabel(key: String, lang: AppLang): String = when (key) {
        "oiliness" -> when (lang) {
            AppLang.EN -> "Oiliness"
            AppLang.JA -> "皮脂バランス"
            AppLang.ZH -> "油脂平衡"
            AppLang.KO -> "유분 밸런스"
        }
        "dark_circle_v2", "dark_circle" -> when (lang) {
            AppLang.EN -> "Dark circles"
            AppLang.JA -> "クマ"
            AppLang.ZH -> "黑眼圈"
            AppLang.KO -> "다크서클"
        }
        "moisture" -> when (lang) {
            AppLang.EN -> "Moisture"
            AppLang.JA -> "水分量"
            AppLang.ZH -> "水分含量"
            AppLang.KO -> "수분도"
        }
        "redness" -> when (lang) {
            AppLang.EN -> "Redness"
            AppLang.JA -> "赤み"
            AppLang.ZH -> "泛红"
            AppLang.KO -> "붉은기"
        }
        "pore" -> when (lang) {
            AppLang.EN -> "Pores"
            AppLang.JA -> "毛穴"
            AppLang.ZH -> "毛孔"
            AppLang.KO -> "모공"
        }
        "acne" -> when (lang) {
            AppLang.EN -> "Acne"
            AppLang.JA -> "ニキビ"
            AppLang.ZH -> "痘痘"
            AppLang.KO -> "트러블"
        }
        "wrinkle" -> when (lang) {
            AppLang.EN -> "Wrinkles"
            AppLang.JA -> "シワ"
            AppLang.ZH -> "皱纹"
            AppLang.KO -> "주름"
        }
        "texture" -> when (lang) {
            AppLang.EN -> "Texture"
            AppLang.JA -> "キメ"
            AppLang.ZH -> "肤质细腻度"
            AppLang.KO -> "피부결"
        }
        "age_spot" -> when (lang) {
            AppLang.EN -> "Age spots"
            AppLang.JA -> "シミ・色素"
            AppLang.ZH -> "色斑"
            AppLang.KO -> "잡티·색소"
        }
        else -> key
    }

    fun dataSourceLabel(source: DataSource, lang: AppLang): String = when (source) {
        DataSource.YOUCAM -> when (lang) {
            AppLang.EN -> "YouCam Skin AI"
            AppLang.JA -> "YouCam AI解析"
            AppLang.ZH -> "YouCam AI分析"
            AppLang.KO -> "YouCam 피부 AI"
        }
        DataSource.HEALTH_CONNECT -> when (lang) {
            AppLang.EN -> "Health Connect"
            AppLang.JA -> "ヘルスコネクト"
            AppLang.ZH -> "Health Connect"
            AppLang.KO -> "헬스 커넥트"
        }
        DataSource.RULE_TABLE -> when (lang) {
            AppLang.EN -> "Rule Table"
            AppLang.JA -> "成分ルール表"
            AppLang.ZH -> "成分规则表"
            AppLang.KO -> "성분 규칙표"
        }
        DataSource.MASK_GRID -> when (lang) {
            AppLang.EN -> "Mask Grid"
            AppLang.JA -> "マスク分布"
            AppLang.ZH -> "掩膜网格"
            AppLang.KO -> "마스크 격자"
        }
        DataSource.LOCAL_DELTA -> when (lang) {
            AppLang.EN -> "Local Delta"
            AppLang.JA -> "前回比"
            AppLang.ZH -> "本地对比"
            AppLang.KO -> "이전 스캔 비교"
        }
        DataSource.NOT_AVAILABLE -> when (lang) {
            AppLang.EN -> "No Data"
            AppLang.JA -> "データなし"
            AppLang.ZH -> "无数据"
            AppLang.KO -> "데이터 없음"
        }
    }

    // Camera & Main HUD
    fun clinicalAiBadge(lang: AppLang) = when (lang) {
        AppLang.EN -> "SKIN AI"
        AppLang.JA -> "スキンケア AI"
        AppLang.ZH -> "护肤 AI"
        AppLang.KO -> "스킨케어 AI"
    }

    fun scanMyFaceBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "📷 Scan My Face"
        AppLang.JA -> "📷 自分の顔を撮影"
        AppLang.ZH -> "📷 拍摄我的脸"
        AppLang.KO -> "📷 내 얼굴 촬영"
    }

    fun demoSampleBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Demo Sample"
        AppLang.JA -> "デモ見本"
        AppLang.ZH -> "演示样本"
        AppLang.KO -> "데모 샘플"
    }

    fun demoModeBadge(lang: AppLang) = when (lang) {
        AppLang.EN -> "Demo Sample (Not Saved)"
        AppLang.JA -> "デモ見本（履歴保存なし）"
        AppLang.ZH -> "演示样本（不计入历史）"
        AppLang.KO -> "데모 샘플 (내 기록 미반영)"
    }

    fun readingSleep(lang: AppLang) = when (lang) {
        AppLang.EN -> "Reading Health Connect sleep data..."
        AppLang.JA -> "ヘルスコネクトの睡眠データを読み込み中..."
        AppLang.ZH -> "正在读取 Health Connect 睡眠数据..."
        AppLang.KO -> "헬스 커넥트 수면 데이터 읽는 중..."
    }

    fun latestSleep(hours: Double, isDeficit: Boolean, lang: AppLang) = when (lang) {
        AppLang.EN -> "Latest sleep: ${hours}h" + if (isDeficit) " · below 6.5h" else ""
        AppLang.JA -> "直近の睡眠: ${hours}時間" + if (isDeficit) " · 6.5h未満" else ""
        AppLang.ZH -> "最近睡眠: ${hours}小时" + if (isDeficit) " · 低于6.5h" else ""
        AppLang.KO -> "최근 수면: ${hours}시간" + if (isDeficit) " · 6.5시간 미만" else ""
    }

    fun sleepAccessNotGranted(lang: AppLang) = when (lang) {
        AppLang.EN -> "Sleep access not granted"
        AppLang.JA -> "睡眠データの権限が未許可です"
        AppLang.ZH -> "未授予睡眠数据权限"
        AppLang.KO -> "수면 데이터 권한이 허용되지 않음"
    }

    fun grantBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Grant"
        AppLang.JA -> "許可"
        AppLang.ZH -> "授权"
        AppLang.KO -> "허용"
    }

    fun noSleep24h(lang: AppLang) = when (lang) {
        AppLang.EN -> "No sleep session recorded in the past 24h"
        AppLang.JA -> "過去24時間の睡眠記録がありません"
        AppLang.ZH -> "过去24小时内无睡眠记录"
        AppLang.KO -> "최근 24시간 내 기록된 수면 세션 없음"
    }

    fun healthConnectUnavailable(lang: AppLang) = when (lang) {
        AppLang.EN -> "Health Connect not available on this device"
        AppLang.JA -> "この端末ではヘルスコネクトを利用できません"
        AppLang.ZH -> "此设备不支持 Health Connect"
        AppLang.KO -> "이 기기에서 헬스 커넥트를 사용할 수 없음"
    }

    fun cameraPermRequired(lang: AppLang) = when (lang) {
        AppLang.EN -> "Camera access is required for live skin wellness analysis."
        AppLang.JA -> "リアルタイム肌分析にはカメラ権限が必要です。"
        AppLang.ZH -> "实时肌肤分析需要相机权限。"
        AppLang.KO -> "실시간 피부 분석을 위해 카메라 권한이 필요합니다."
    }

    fun grantCameraBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Grant Camera Access"
        AppLang.JA -> "カメラを許可"
        AppLang.ZH -> "授予相机权限"
        AppLang.KO -> "카메라 권한 허용"
    }

    fun appSettingsBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "App Settings"
        AppLang.JA -> "アプリ設定"
        AppLang.ZH -> "应用设置"
        AppLang.KO -> "앱 설정"
    }

    fun trySampleNoCam(isAnalyzing: Boolean, lang: AppLang) = when (lang) {
        AppLang.EN -> if (isAnalyzing) "Analyzing Sample..." else "Try with Sample Face (No Camera)"
        AppLang.JA -> if (isAnalyzing) "サンプル解析中..." else "サンプル顔写真で試す（カメラ不要）"
        AppLang.ZH -> if (isAnalyzing) "正在分析样本..." else "使用样本人脸体验（无需相机）"
        AppLang.KO -> if (isAnalyzing) "샘플 분석 중..." else "샘플 얼굴로 바로 체험 (카메라 불필요)"
    }

    fun cameraGuideHint(lang: AppLang) = when (lang) {
        AppLang.EN -> "Align face in oval · Sent in-memory to YouCam Skin AI (No local disk save)"
        AppLang.JA -> "楕円に顔を合わせる · メモリ処理でYouCam AIへ送信（端末保存なし）"
        AppLang.ZH -> "将面部对准椭圆框 · 内存处理后发送至 YouCam AI（本地不保存）"
        AppLang.KO -> "타원에 얼굴 맞춤 · 기기 저장 없이 메모리에서 YouCam AI로 전송·분석"
    }

    fun sampleFaceBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Sample Face"
        AppLang.JA -> "サンプル顔"
        AppLang.ZH -> "样本测试"
        AppLang.KO -> "샘플 얼굴"
    }

    fun stageText(step: Int, lang: AppLang) = when (step) {
        1 -> when (lang) {
            AppLang.EN -> "1/3 Photo captured! Preparing 600x600 frame..."
            AppLang.JA -> "1/3 撮影完了！600x600フレームを準備中..."
            AppLang.ZH -> "1/3 拍摄完成！正在准备 600x600 图像帧..."
            AppLang.KO -> "1/3 사진 촬영 완료! 600x600 프레임 준비 중..."
        }
        2 -> when (lang) {
            AppLang.EN -> "2/3 Photo captured! You may lower your phone (~5-8s AI analysis)..."
            AppLang.JA -> "2/3 撮影完了！スマホを下ろしてお待ちください（AI解析 約5〜8秒）..."
            AppLang.ZH -> "2/3 拍摄完成！您可以放下手机等待（AI分析约5-8秒）..."
            AppLang.KO -> "2/3 사진 촬영 완료! 이제 휴대폰을 내려놓으셔도 됩니다 (AI 분석 약 5~8초)..."
        }
        3 -> when (lang) {
            AppLang.EN -> "3/3 Matching ingredient rules..."
            AppLang.JA -> "3/3 スキンケア成分ルールを照合中..."
            AppLang.ZH -> "3/3 正在匹配护肤成分规则..."
            AppLang.KO -> "3/3 맞춤 스킨케어 성분 규칙 매칭 중..."
        }
        else -> ""
    }

    fun capturedHoldBadge(lang: AppLang) = when (lang) {
        AppLang.EN -> "✓ Photo Captured! You can lower your phone now (~5-8s)"
        AppLang.JA -> "✓ 撮影完了！スマホを下ろしてお待ちください（約5〜8秒）"
        AppLang.ZH -> "✓ 拍摄完成！现在可以放下手机（约5-8秒）"
        AppLang.KO -> "✓ 사진 촬영 완료! 이제 휴대폰을 편하게 내려놓으세요 (약 5~8초)"
    }

    // Result Screen Top & Hero
    fun rescanBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Rescan"
        AppLang.JA -> "再測定"
        AppLang.ZH -> "重新扫描"
        AppLang.KO -> "재촬영"
    }

    fun skinScoreLabel(lang: AppLang) = when (lang) {
        AppLang.EN -> "SKIN SCORE"
        AppLang.JA -> "総合スコア"
        AppLang.ZH -> "肌肤总分"
        AppLang.KO -> "피부 종합점수"
    }

    fun maskBadge(active: Boolean, count: Int, lang: AppLang) = when (lang) {
        AppLang.EN -> if (active) "Mask Active" else "$count AI Masks"
        AppLang.JA -> if (active) "マスク表示中" else "AIマスク ${count}種"
        AppLang.ZH -> if (active) "掩膜已激活" else "${count}项 AI掩膜"
        AppLang.KO -> if (active) "마스크 켜짐" else "AI 마스크 ${count}종"
    }

    fun kpiSkinAgeLabel(lang: AppLang) = when (lang) {
        AppLang.EN -> "EST. SKIN AGE"
        AppLang.JA -> "推定肌年齢"
        AppLang.ZH -> "预估肌龄"
        AppLang.KO -> "추정 피부나이"
    }

    fun kpiSkinAgeValue(age: Int?, lang: AppLang) = if (age == null) "N/A" else when (lang) {
        AppLang.EN -> "$age yrs"
        AppLang.JA -> "${age}歳"
        AppLang.ZH -> "${age}岁"
        AppLang.KO -> "${age}세"
    }

    fun kpiSleepLabel(lang: AppLang) = when (lang) {
        AppLang.EN -> "SLEEP RECOVERY"
        AppLang.JA -> "睡眠リカバリー"
        AppLang.ZH -> "睡眠恢复"
        AppLang.KO -> "수면 회복도"
    }

    fun kpiSleepValue(hours: Double?, lang: AppLang) = if (hours == null) {
        when (lang) {
            AppLang.EN -> "No Data"
            AppLang.JA -> "未連携"
            AppLang.ZH -> "无数据"
            AppLang.KO -> "기록 없음"
        }
    } else {
        when (lang) {
            AppLang.EN -> "${hours}h"
            AppLang.JA -> "${hours}時間"
            AppLang.ZH -> "${hours}小时"
            AppLang.KO -> "${hours}시간"
        }
    }

    fun kpiDeltaLabel(lang: AppLang) = when (lang) {
        AppLang.EN -> "SCAN DELTA"
        AppLang.JA -> "前回測定比"
        AppLang.ZH -> "扫描变化"
        AppLang.KO -> "이전 스캔 대비"
    }

    fun kpiDeltaValue(avgDelta: Int?, lang: AppLang) = when {
        avgDelta == null -> when (lang) {
            AppLang.EN -> "Baseline"
            AppLang.JA -> "初回測定"
            AppLang.ZH -> "首次基准"
            AppLang.KO -> "첫 측정 기준"
        }
        avgDelta > 0 -> "+$avgDelta pts ▲"
        avgDelta < 0 -> "$avgDelta pts ▼"
        else -> "0 pts ─"
    }

    fun originalPhotoChip(lang: AppLang) = when (lang) {
        AppLang.EN -> "Original Photo"
        AppLang.JA -> "元の写真"
        AppLang.ZH -> "原图"
        AppLang.KO -> "원본 사진"
    }

    fun noMaskLayers(lang: AppLang) = when (lang) {
        AppLang.EN -> "Mask layers not provided for this scan."
        AppLang.JA -> "今回の測定ではマスクレイヤーがありません。"
        AppLang.ZH -> "本次扫描未提供掩膜图层。"
        AppLang.KO -> "이번 스캔에서는 마스크 레이어가 제공되지 않았습니다."
    }

    // Radar + Focus Card
    fun radarTitle(lang: AppLang) = when (lang) {
        AppLang.EN -> "SKIN BALANCE RADAR"
        AppLang.JA -> "肌バランスレーダー"
        AppLang.ZH -> "肌肤平衡雷达图"
        AppLang.KO -> "피부 밸런스 레이더"
    }

    fun radarSubtitle(count: Int, lang: AppLang) = when (lang) {
        AppLang.EN -> "$count-axis holistic profile"
        AppLang.JA -> "${count}項目の総合バランス"
        AppLang.ZH -> "${count}维综合肤况分析"
        AppLang.KO -> "${count}대 지표 종합 밸런스"
    }

    fun focusHeader(isHealthy: Boolean, lang: AppLang) = if (isHealthy) {
        when (lang) {
            AppLang.EN -> "OPTIMAL BALANCE"
            AppLang.JA -> "良好なバランス"
            AppLang.ZH -> "状态理想"
            AppLang.KO -> "이상적인 밸런스"
        }
    } else {
        when (lang) {
            AppLang.EN -> "TODAY'S FOCUS"
            AppLang.JA -> "今日の集中ケア"
            AppLang.ZH -> "今日重点护理"
            AppLang.KO -> "오늘의 집중 케어"
        }
    }

    fun focusSubtext(isHealthy: Boolean, count: Int, threshold: Int, lang: AppLang) = if (isHealthy) {
        when (lang) {
            AppLang.EN -> "All $count metrics meet target (≥$threshold)"
            AppLang.JA -> "全${count}項目が目標値(≥$threshold)を達成"
            AppLang.ZH -> "全部${count}项指标均达标 (≥$threshold)"
            AppLang.KO -> "${count}개 지표 모두 목표 기준(≥$threshold) 충족"
        }
    } else {
        when (lang) {
            AppLang.EN -> "Below target ≥$threshold · Tap to view layer"
            AppLang.JA -> "目標値 ≥$threshold 未満 · タップでマスク表示"
            AppLang.ZH -> "低于目标值 ≥$threshold · 点击查看掩膜"
            AppLang.KO -> "권장 기준(≥$threshold) 미만 · 탭하여 마스크 보기"
        }
    }

    fun showMaskLink(metricName: String, lang: AppLang) = when (lang) {
        AppLang.EN -> "Show $metricName mask ▶"
        AppLang.JA -> "${metricName}マスクを表示 ▶"
        AppLang.ZH -> "查看${metricName}掩膜 ▶"
        AppLang.KO -> "$metricName 마스크 보기 ▶"
    }

    fun spatialGridTitle(metricName: String, lang: AppLang) = when (lang) {
        AppLang.EN -> "Spatial Grid ($metricName)"
        AppLang.JA -> "部位別比率 ($metricName)"
        AppLang.ZH -> "区域分布 ($metricName)"
        AppLang.KO -> "부위별 분포 ($metricName)"
    }

    fun spatialGridRatioText(tShare: Int, uShare: Int, lang: AppLang): String {
        val dom = when {
            tShare >= 58 -> when (lang) {
                AppLang.EN -> "Center-heavy"
                AppLang.JA -> "Tゾーン集中"
                AppLang.ZH -> "T区集中"
                AppLang.KO -> "중앙(T존) 집중"
            }
            uShare >= 58 -> when (lang) {
                AppLang.EN -> "Outer-heavy"
                AppLang.JA -> "Uゾーン集中"
                AppLang.ZH -> "U区集中"
                AppLang.KO -> "외곽(U존) 집중"
            }
            else -> when (lang) {
                AppLang.EN -> "Balanced"
                AppLang.JA -> "均等分布"
                AppLang.ZH -> "分布均匀"
                AppLang.KO -> "고른 분포"
            }
        }
        return when (lang) {
            AppLang.EN -> "Center $tShare% : Outer $uShare% ($dom)"
            AppLang.JA -> "中央 $tShare% : 外側 $uShare% ($dom)"
            AppLang.ZH -> "中心 $tShare% : 外围 $uShare% ($dom)"
            AppLang.KO -> "중앙 $tShare% : 외곽 $uShare% ($dom)"
        }
    }

    // 9-Vector Bento Grid
    fun bentoTitle(count: Int, lang: AppLang) = when (lang) {
        AppLang.EN -> "9-VECTOR METRIC BENTO GRID ($count/9 · Higher = Better)"
        AppLang.JA -> "9項目肌スコア詳細 ($count/9 · 高いほど良好)"
        AppLang.ZH -> "9维肌肤指标详情 ($count/9 · 分数越高越好)"
        AppLang.KO -> "9대 피부 지표 상세 ($count/9 · 높을수록 양호)"
    }

    fun bentoDeltaHint(prevTime: String, lang: AppLang) = when (lang) {
        AppLang.EN -> "Delta (▲/▼) vs last scan ($prevTime). Tap any card to toggle mask."
        AppLang.JA -> "前回測定($prevTime)との比較(▲/▼)。カードをタップでマスク切替。"
        AppLang.ZH -> "与上次扫描($prevTime)对比(▲/▼)。点击卡片切换掩膜。"
        AppLang.KO -> "이전 측정($prevTime) 대비 증감(▲/▼). 카드를 누르면 마스크가 켜집니다."
    }

    // Ingredient Safety Guard
    fun safetyGuardTitle(lang: AppLang) = when (lang) {
        AppLang.EN -> "INGREDIENT SAFETY GUARD (O/X)"
        AppLang.JA -> "化粧品成分 相性チェック (O/X)"
        AppLang.ZH -> "化妆品成分适配检测 (O/X)"
        AppLang.KO -> "화장품 전성분 궁합 스캐너 (O/X)"
    }

    fun safetyGuardSub(lang: AppLang) = when (lang) {
        AppLang.EN -> "Cross-checks INCI ingredients against your measured skin scores. Reference only; does not evaluate allergy."
        AppLang.JA -> "測定された肌スコアと全成分(INCI)を照合します（※参考情報でありアレルギー判定ではありません）。"
        AppLang.ZH -> "将化妆品全成分(INCI)与您的实测肌肤分数进行比对（仅供参考，不评估个人过敏）。"
        AppLang.KO -> "측정된 내 피부 점수와 화장품 전성분(INCI)을 대조합니다 (참고용이며 개인 알레르기·농도는 판별하지 않음)."
    }

    fun presetCica(lang: AppLang) = when (lang) {
        AppLang.EN -> "Preset: Cica Serum"
        AppLang.JA -> "例: シカ美容液"
        AppLang.ZH -> "示例: 积雪草精华"
        AppLang.KO -> "샘플: 시카 진정 세럼"
    }

    fun presetAlcohol(lang: AppLang) = when (lang) {
        AppLang.EN -> "Preset: Alcohol Toner"
        AppLang.JA -> "例: アルコール化粧水"
        AppLang.ZH -> "示例: 含酒精爽肤水"
        AppLang.KO -> "샘플: 알코올 토너"
    }

    fun presetOilBalm(lang: AppLang) = when (lang) {
        AppLang.EN -> "Preset: Heavy Oil Balm"
        AppLang.JA -> "例: リッチオイルバーム"
        AppLang.ZH -> "示例: 厚重椰子油膏"
        AppLang.KO -> "샘플: 고보습 오일 밤"
    }

    fun productPlaceholder(lang: AppLang) = when (lang) {
        AppLang.EN -> "Product name (optional)..."
        AppLang.JA -> "製品名（任意）..."
        AppLang.ZH -> "产品名称（选填）..."
        AppLang.KO -> "제품명 입력 (선택)..."
    }

    fun inciPlaceholder(lang: AppLang) = when (lang) {
        AppLang.EN -> "Paste comma-separated INCI ingredients or tap a preset..."
        AppLang.JA -> "カンマ区切りの成分を入力するか、上のサンプルをタップ..."
        AppLang.ZH -> "粘贴逗号分隔的成分表，或点击上方示例..."
        AppLang.KO -> "쉼표로 구분된 전성분을 붙여넣거나 위 샘플 버튼을 누르세요..."
    }

    fun analyzeCompatBtn(checking: Boolean, lang: AppLang) = when (lang) {
        AppLang.EN -> if (checking) "Checking Formula..." else "Analyze Compatibility with My Skin"
        AppLang.JA -> if (checking) "成分を照合中..." else "自分の肌との相性を判定する"
        AppLang.ZH -> if (checking) "正在分析配方..." else "分析与我肌肤的适配度"
        AppLang.KO -> if (checking) "성분 궁합 분석 중..." else "내 피부와 성분 궁합 분석하기"
    }

    fun verdictBadgeText(verdict: String, lang: AppLang) = when (verdict) {
        "GOOD_MATCH" -> when (lang) {
            AppLang.EN -> "SUPPORTIVE MATCH"
            AppLang.JA -> "相性良好 (おすすめ)"
            AppLang.ZH -> "非常适合"
            AppLang.KO -> "성분 궁합 좋음"
        }
        "MIXED_CAUTION" -> when (lang) {
            AppLang.EN -> "MIXED (BENEFITS + CAUTION)"
            AppLang.JA -> "一部注意 (有効+注意成分)"
            AppLang.ZH -> "喜忧参半 (含注意成分)"
            AppLang.KO -> "유익 성분 + 주의 성분 혼재"
        }
        "CONFLICT" -> when (lang) {
            AppLang.EN -> "CONFLICT DETECTED"
            AppLang.JA -> "肌状態と衝突あり"
            AppLang.ZH -> "检测到冲突成分"
            AppLang.KO -> "피부 취약점 충돌 감지"
        }
        "CAUTION" -> when (lang) {
            AppLang.EN -> "CAUTION ADVISED"
            AppLang.JA -> "パッチテスト推奨"
            AppLang.ZH -> "建议局部测试"
            AppLang.KO -> "패치 테스트 권장"
        }
        else -> when (lang) {
            AppLang.EN -> "NO KEYWORD MATCH"
            AppLang.JA -> "該当キーワードなし"
            AppLang.ZH -> "无匹配关键词"
            AppLang.KO -> "일치하는 규칙 키워드 없음"
        }
    }

    fun cosmeticSummaryLocalized(cr: CosmeticCheckResult, lang: AppLang): String {
        if (lang == AppLang.EN) return cr.summary
        val cCount = cr.conflicts.size
        val wCount = cr.cautions.size
        val bCount = cr.benefits.size
        return when (cr.verdict) {
            "CONFLICT" -> when (lang) {
                AppLang.JA -> "目標値未満の肌項目に対して、注意すべき衝突成分が ${cCount}件 検出されました。"
                AppLang.ZH -> "针对您低于目标值的肌肤指标，检测到 ${cCount} 项潜在冲突成分。"
                AppLang.KO -> "현재 기준치 미만인 피부 취약 지표와 충돌할 수 있는 성분이 ${cCount}건 감지되었습니다."
                else -> cr.summary
            }
            "MIXED_CAUTION" -> when (lang) {
                AppLang.JA -> "肌を助ける成分が ${bCount}件 含まれていますが、注意成分も ${wCount}件 あります。"
                AppLang.ZH -> "包含 ${bCount} 项有益成分，同时存在 ${wCount} 项需注意成分，使用前请留意。"
                AppLang.KO -> "도움이 되는 유익 성분 ${bCount}건과 주의 성분 ${wCount}건이 함께 포함되어 있습니다."
                else -> cr.summary
            }
            "CAUTION" -> when (lang) {
                AppLang.JA -> "刺激の可能性がある成分が ${wCount}件 検出されました。パッチテストを推奨します。"
                AppLang.ZH -> "检测到 ${wCount} 项潜在刺激或待确认成分，建议先做局部测试。"
                AppLang.KO -> "자극 가능성이 있거나 확인이 필요한 성분이 ${wCount}건 감지되어 패치 테스트를 권장합니다."
                else -> cr.summary
            }
            "GOOD_MATCH" -> when (lang) {
                AppLang.JA -> "現在の肌スコアに役立つ有効成分が ${bCount}件 一致し、衝突成分は検出されませんでした。"
                AppLang.ZH -> "匹配到 ${bCount} 项有益活性成分，且未检测到冲突关键词。"
                AppLang.KO -> "내 피부에 도움이 되는 핵심 성분 ${bCount}건이 일치하며, 충돌 키워드가 발견되지 않았습니다."
                else -> cr.summary
            }
            else -> when (lang) {
                AppLang.JA -> "ルール表の衝突・有効キーワードに該当する成分はありませんでした。"
                AppLang.ZH -> "所提供的成分表中未匹配到规则库中的冲突或有益关键词。"
                AppLang.KO -> "입력된 전성분에서 규칙 테이블의 충돌/유익 키워드가 감지되지 않았습니다."
                else -> cr.summary
            }
        }
    }

    fun itemTagLocalized(tag: String, lang: AppLang) = when (tag) {
        "AVOID" -> when (lang) {
            AppLang.EN -> "AVOID"
            AppLang.JA -> "非推奨"
            AppLang.ZH -> "避雷"
            AppLang.KO -> "주의/회피"
        }
        "CAUTION" -> when (lang) {
            AppLang.EN -> "CAUTION"
            AppLang.JA -> "注意"
            AppLang.ZH -> "谨慎"
            AppLang.KO -> "패치권장"
        }
        else -> when (lang) {
            AppLang.EN -> "GOOD"
            AppLang.JA -> "適合"
            AppLang.ZH -> "有益"
            AppLang.KO -> "유익"
        }
    }

    fun localizeItemReason(item: CosmeticMatchItem, tag: String, lang: AppLang): String {
        if (lang == AppLang.EN) return item.reason
        val mLabel = metricLabel(item.metric, lang)
        return when (tag) {
            "AVOID" -> when (lang) {
                AppLang.JA -> "${mLabel}スコアが基準未満のため、刺激や負担になる可能性があります"
                AppLang.ZH -> "当前${mLabel}分数低于目标值，可能引起刺激或负担"
                AppLang.KO -> "현재 ${mLabel} 점수가 권장 기준 미만이어서 자극이나 부담을 줄 수 있습니다"
                else -> item.reason
            }
            "CAUTION" -> when (lang) {
                AppLang.JA -> "${mLabel}に関連する注意成分です。使用前のパッチテストを推奨します"
                AppLang.ZH -> "与${mLabel}相关的敏感成分，建议局部测试后使用"
                AppLang.KO -> "${mLabel} 관련 주의 성분입니다. 사용 전 국소 패치 테스트를 권장합니다"
                else -> item.reason
            }
            else -> when (lang) {
                AppLang.JA -> "${mLabel}のケアとバリア維持をサポートする有効成分です"
                AppLang.ZH -> "有助于改善和维持${mLabel}状态的活性成分"
                AppLang.KO -> "${mLabel} 케어 및 피부 장벽 유지에 도움을 주는 성분입니다"
                else -> item.reason
            }
        }
    }

    fun expandCollapseItems(expanded: Boolean, count: Int, lang: AppLang) = if (expanded) {
        when (lang) {
            AppLang.EN -> "Collapse details ▲"
            AppLang.JA -> "詳細を閉じる ▲"
            AppLang.ZH -> "收起详情 ▲"
            AppLang.KO -> "상세 내역 접기 ▲"
        }
    } else {
        when (lang) {
            AppLang.EN -> "▼ View $count more matched item(s)"
            AppLang.JA -> "▼ 他 ${count}件 の一致成分をすべて見る"
            AppLang.ZH -> "▼ 查看其余 $count 项匹配成分"
            AppLang.KO -> "▼ 나머지 ${count}개 매칭 성분 더보기"
        }
    }

    // Section 5: Suggested Ingredients & Daily Routine
    fun suggestedSectionTitle(lang: AppLang) = when (lang) {
        AppLang.EN -> "SUGGESTED INGREDIENTS & DAILY ROUTINE"
        AppLang.JA -> "推奨成分 ＆ デイリールーティン"
        AppLang.ZH -> "推荐成分与每日护肤流程"
        AppLang.KO -> "추천 성분 & 데일리 루틴 체크리스트"
    }

    fun retryBtn(retrying: Boolean, lang: AppLang) = when (lang) {
        AppLang.EN -> if (retrying) "Retrying..." else "Retry guidance"
        AppLang.JA -> if (retrying) "再試行中..." else "ガイドを再取得"
        AppLang.ZH -> if (retrying) "正在重试..." else "重新获取建议"
        AppLang.KO -> if (retrying) "재시도 중..." else "가이드 다시 불러오기"
    }

    fun allThresholdsMetMsg(lang: AppLang) = when (lang) {
        AppLang.EN -> "Available rule-evaluated scores meet thresholds. Maintain your current gentle barrier care."
        AppLang.JA -> "評価対象のすべての項目が目標基準を満たしています。現在の優しい保湿ケアを継続してください。"
        AppLang.ZH -> "所有评估指标均已达到目标标准，请继续保持温和的屏障修护护理。"
        AppLang.KO -> "평가된 모든 지표가 권장 기준을 충족합니다. 현재의 순한 장벽 보습 케어를 유지하세요."
    }

    fun localizeSuggestionTag(metricKey: String, fallbackTag: String, lang: AppLang): String {
        if (lang == AppLang.EN) return fallbackTag
        return when (metricKey) {
            "oiliness" -> when (lang) {
                AppLang.JA -> "Tゾーン皮脂＆毛穴ケア"
                AppLang.ZH -> "T区控油与毛孔护理"
                AppLang.KO -> "T존 유분 & 모공 케어"
                else -> fallbackTag
            }
            "dark_circle" -> when (lang) {
                AppLang.JA -> "目元の疲れ＆クマケア"
                AppLang.ZH -> "眼周疲劳与黑眼圈护理"
                AppLang.KO -> "눈가 피로 & 다크서클 케어"
                else -> fallbackTag
            }
            "moisture" -> when (lang) {
                AppLang.JA -> "深層保湿＆バリア補給"
                AppLang.ZH -> "深层补水与屏障修护"
                AppLang.KO -> "속보습 & 피부 장벽 케어"
                else -> fallbackTag
            }
            "redness" -> when (lang) {
                AppLang.JA -> "鎮静＆赤みケア"
                AppLang.ZH -> "舒缓泛红与屏障修护"
                AppLang.KO -> "진정 & 붉은기 완화 케어"
                else -> fallbackTag
            }
            "pore" -> when (lang) {
                AppLang.JA -> "毛穴引き締め＆キメケア"
                AppLang.ZH -> "毛孔细致与肤质改善"
                AppLang.KO -> "모공 수렴 & 피부결 정돈"
                else -> fallbackTag
            }
            else -> fallbackTag
        }
    }

    fun localizeSuggestionPurpose(metricKey: String, fallbackPurpose: String, lang: AppLang): String {
        if (lang == AppLang.EN) return fallbackPurpose
        return when (metricKey) {
            "oiliness" -> when (lang) {
                AppLang.JA -> "Tゾーンの詰まった毛穴をケアし、皮脂バランスを整えます"
                AppLang.ZH -> "疏通T区堵塞毛孔并平衡油脂分泌"
                AppLang.KO -> "막힌 T존 모공을 정돈하고 과잉 유분 밸런스를 맞춰줍니다"
                else -> fallbackPurpose
            }
            "dark_circle" -> when (lang) {
                AppLang.JA -> "目元のむくみとクマの印象をすっきりとケアします"
                AppLang.ZH -> "改善眼周浮肿并淡化黑眼圈外观"
                AppLang.KO -> "눈가 붓기와 다크서클 피로 흔적을 집중 케어합니다"
                else -> fallbackPurpose
            }
            "moisture" -> when (lang) {
                AppLang.JA -> "角質層まで素早く潤いを届け、水分バリアを補給します"
                AppLang.ZH -> "即时深层补水并修护肌肤保湿屏障"
                AppLang.KO -> "즉각적인 속보습을 공급하고 수분 장벽을 탄탄하게 채워줍니다"
                else -> fallbackPurpose
            }
            "redness" -> when (lang) {
                AppLang.JA -> "敏感になった肌を穏やかに鎮静し、赤みを和らげます"
                AppLang.ZH -> "温和舒缓受刺激肌肤并减轻面部泛红"
                AppLang.KO -> "자극받은 피부를 진정시키고 붉은기를 순하게 완화합니다"
                else -> fallbackPurpose
            }
            "pore" -> when (lang) {
                AppLang.JA -> "広がった毛穴を引き締め、なめらかな肌キメに整えます"
                AppLang.ZH -> "收敛粗大毛孔并改善不均匀肤质"
                AppLang.KO -> "늘어진 모공을 조여주고 거친 피부결을 매끄럽게 정돈합니다"
                else -> fallbackPurpose
            }
            else -> fallbackPurpose
        }
    }

    fun suggestionScoreReason(tag: String, score: Int?, threshold: Int?, lang: AppLang): String {
        if (score == null || threshold == null) return tag
        return if (score < threshold) {
            when (lang) {
                AppLang.EN -> "$tag · Priority ($score < $threshold)"
                AppLang.JA -> "$tag · 集中ケア ($score < $threshold)"
                AppLang.ZH -> "$tag · 重点修护 ($score < $threshold)"
                AppLang.KO -> "$tag · 집중 케어 ($score < $threshold)"
            }
        } else {
            when (lang) {
                AppLang.EN -> "$tag · Optional Maintenance Explore ($score / target $threshold+)"
                AppLang.JA -> "$tag · 任意維持ケア探索 ($score / 基準 $threshold+)"
                AppLang.ZH -> "$tag · 可选日常维稳探索 ($score / 标准 $threshold+)"
                AppLang.KO -> "$tag · 선택적 유지 보습 탐색 ($score / 기준 $threshold+)"
            }
        }
    }

    fun exploreProductBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "🛍️ Shop / Explore"
        AppLang.JA -> "🛍️ 製品を探す"
        AppLang.ZH -> "🛍️ 查看产品"
        AppLang.KO -> "🛍️ 제품 보러가기"
    }

    fun localizeSuggestionIngredients(metricKey: String, fallbackActive: String, lang: AppLang): String {
        if (lang == AppLang.EN) return fallbackActive
        return when (metricKey) {
            "oiliness" -> when (lang) {
                AppLang.JA -> "サリチル酸 (BHA 2%)、緑茶エキス"
                AppLang.ZH -> "水杨酸 (BHA 2%)、绿茶提取物"
                AppLang.KO -> "살리실산 (BHA 2%), 녹차추출물"
                else -> fallbackActive
            }
            "dark_circle" -> when (lang) {
                AppLang.JA -> "カフェイン (5%)、EGCG（緑茶カテキン）"
                AppLang.ZH -> "咖啡因 (5%)、EGCG 茶多酚"
                AppLang.KO -> "카페인 (5%), EGCG 녹차 카테킨"
                else -> fallbackActive
            }
            "moisture" -> when (lang) {
                AppLang.JA -> "5D複合ヒアルロン酸、D-パンテノール、アラントイン"
                AppLang.ZH -> "5D复合透明质酸、泛醇 (B5)、尿囊素"
                AppLang.KO -> "5D 복합 저분자 히알루론산, 판테놀, 알란토인"
                else -> fallbackActive
            }
            "redness" -> when (lang) {
                AppLang.JA -> "ツボクサエキス (CICA 100%)、マデカシン酸"
                AppLang.ZH -> "积雪草提取物 (100%)、羟基积雪草酸"
                AppLang.KO -> "병풀추출물 (센텔라 100%), 마데카식애씨드"
                else -> fallbackActive
            }
            "pore" -> when (lang) {
                AppLang.JA -> "ナイアシンアミド (ビタミンB3 10%)、PCA亜鉛 (1%)"
                AppLang.ZH -> "烟酰胺 (维生素B3 10%)、PCA锌 (1%)"
                AppLang.KO -> "나이아신아마이드 (비타민B3 10%), 징크 PCA (1%)"
                else -> fallbackActive
            }
            else -> fallbackActive
        }
    }

    fun localizeSuggestionProduct(metricKey: String, fallbackBrand: String, fallbackName: String, lang: AppLang): String {
        val defaultText = "$fallbackBrand · $fallbackName"
        if (lang == AppLang.EN) return defaultText
        return when (metricKey) {
            "oiliness" -> when (lang) {
                AppLang.JA -> "ポーラチョイス · BHA リキッド 2%"
                AppLang.ZH -> "宝拉珍选 · 2%水杨酸精华液"
                AppLang.KO -> "폴라초이스 · 스킨 퍼펙팅 바하(BHA) 리퀴드"
                else -> defaultText
            }
            "dark_circle" -> when (lang) {
                AppLang.JA -> "ジオーディナリー · カフェインソリューション 5% + EGCG"
                AppLang.ZH -> "The Ordinary · 5%咖啡因 + EGCG 眼部精华"
                AppLang.KO -> "디오디너리 · 카페인 솔루션 5% + EGCG"
                else -> defaultText
            }
            "moisture" -> when (lang) {
                AppLang.JA -> "トリデン (Torriden) · ダイブイン セラム"
                AppLang.ZH -> "桃瑞丹 (Torriden) · 低分子玻尿酸精华"
                AppLang.KO -> "토리든 · 다이브인 저분자 히알루론산 세럼"
                else -> defaultText
            }
            "redness" -> when (lang) {
                AppLang.JA -> "SKIN1004 · マダガスカル センテラ アンプル"
                AppLang.ZH -> "SKIN1004 · 马达加斯加积雪草安瓶精华"
                AppLang.KO -> "스킨1004 · 마다가스카르 센텔라 앰플"
                else -> defaultText
            }
            "pore" -> when (lang) {
                AppLang.JA -> "ジオーディナリー · ナイアシンアミド 10% + 亜鉛 1%"
                AppLang.ZH -> "The Ordinary · 10%烟酰胺 + 1%锌精华"
                AppLang.KO -> "디오디너리 · 나이아신아마이드 10% + 징크 1%"
                else -> defaultText
            }
            else -> defaultText
        }
    }

    fun localizeSuggestionSearchUrl(metricKey: String, fallbackUrl: String, lang: AppLang): String {
        if (lang == AppLang.EN) {
            return if (fallbackUrl.isNotBlank()) "$fallbackUrl&hl=en" else ""
        }
        return when (metricKey) {
            "oiliness" -> when (lang) {
                AppLang.JA -> "https://www.google.co.jp/search?hl=ja&q=%E3%83%9D%E3%83%BC%E3%83%A9%E3%83%81%E3%83%A7%E3%82%A4%E3%82%B9+BHA+%E3%83%AA%E3%82%AD%E3%83%83%E3%83%89"
                AppLang.ZH -> "https://www.google.com/search?hl=zh-CN&q=%E5%AE%9D%E6%8B%89%E7%8F%8D%E9%80%89+2%25%E6%B0%B4%E6%9D%A8%E9%85%B8%E7%B2%BE%E5%8D%8E%E6%B6%B2"
                AppLang.KO -> "https://www.google.co.kr/search?hl=ko&q=%ED%8F%B4%EB%9D%BC%EC%B4%88%EC%9D%B4%EC%8A%A4+%EC%8A%A4%ED%82%A8+%ED%8D%BC%ED%8E%99%ED%8C%85+%EB%B0%94%ED%95%98+%EB%A6%AC%ED%80%B4%EB%93%9C"
                else -> fallbackUrl
            }
            "dark_circle" -> when (lang) {
                AppLang.JA -> "https://www.google.co.jp/search?hl=ja&q=The+Ordinary+%E3%82%AB%E3%83%95%E3%82%A7%E3%82%A4%E3%83%B3%E3%82%BD%E3%83%AA%E3%83%A5%E3%83%BC%E3%82%B7%E3%83%A7%E3%83%B3+5%25+EGCG"
                AppLang.ZH -> "https://www.google.com/search?hl=zh-CN&q=The+Ordinary+5%25%E5%92%96%E5%95%A1%E5%9B%A0+EGCG+%E7%9C%BC%E9%83%A8%E7%B2%BE%E5%8D%8E"
                AppLang.KO -> "https://www.google.co.kr/search?hl=ko&q=%EB%94%94%EC%98%A4%EB%94%94%EB%84%88%EB%A6%AC+%EC%B9%B4%ED%8E%98%EC%9D%B8+%EC%86%94%EB%A3%A8%EC%85%98+5%25+EGCG"
                else -> fallbackUrl
            }
            "moisture" -> when (lang) {
                AppLang.JA -> "https://www.google.co.jp/search?hl=ja&q=%E3%83%88%E3%83%AA%E3%83%87%E3%83%B3+%E3%83%80%E3%82%A4%E3%83%96%E3%82%A4%E3%83%B3+%E3%82%BB%E3%83%A9%E3%83%A0"
                AppLang.ZH -> "https://www.google.com/search?hl=zh-CN&q=%E6%A1%83%E7%91%9E%E4%B8%B9+Torriden+%E4%BD%8E%E5%88%86%E5%AD%90%E7%8E%BB%E5%B0%BF%E9%85%B8%E7%B2%BE%E5%8D%8E"
                AppLang.KO -> "https://www.google.co.kr/search?hl=ko&q=%ED%86%A0%EB%A6%AC%EB%93%A0+%EB%8B%A4%EC%9D%B4%EB%B8%8C%EC%9D%B8+%EC%A0%80%EB%B6%84%EC%9E%90+%ED%9E%88%EC%95%8C%EB%A3%A8%EB%A1%A0%EC%82%B0+%EC%84%B8%EB%9F%BC"
                else -> fallbackUrl
            }
            "redness" -> when (lang) {
                AppLang.JA -> "https://www.google.co.jp/search?hl=ja&q=SKIN1004+%E3%83%9E%E3%83%80%E3%82%AC%E3%82%B9%E3%82%AB%E3%83%AB+%E3%82%BB%E3%83%B3%E3%83%86%E3%83%A9+%E3%82%A2%E3%83%B3%E3%83%97%E3%83%AB"
                AppLang.ZH -> "https://www.google.com/search?hl=zh-CN&q=SKIN1004+%E9%A9%AC%E8%BE%BE%E5%8A%A0%E6%96%AF%E5%8A%A0%E7%A7%AF%E9%9B%AA%E8%8D%89%E5%AE%89%E7%93%B6%E7%B2%BE%E5%8D%8E"
                AppLang.KO -> "https://www.google.co.kr/search?hl=ko&q=%EC%8A%A4%ED%82%A81004+%EB%A7%88%EB%8B%A4%EA%B0%80%EC%8A%A4%EC%B9%B4%EB%A5%B4+%EC%84%BC%ED%85%94%EB%9D%BC+%EC%95%B0%ED%94%8C"
                else -> fallbackUrl
            }
            "pore" -> when (lang) {
                AppLang.JA -> "https://www.google.co.jp/search?hl=ja&q=The+Ordinary+%E3%83%8A%E3%82%A4%E3%82%A2%E3%82%B7%E3%83%B3%E3%82%A2%E3%83%9F%E3%83%89+10%25+%E4%BA%9C%E9%89%9B+1%25"
                AppLang.ZH -> "https://www.google.com/search?hl=zh-CN&q=The+Ordinary+10%25%E7%83%9F%E9%85%B0%E8%83%BA+1%25%E9%94%8C%E7%B2%BE%E5%8D%8E"
                AppLang.KO -> "https://www.google.co.kr/search?hl=ko&q=%EB%94%94%EC%98%A4%EB%94%94%EB%84%88%EB%A6%AC+%EB%82%98%EC%9D%B4%EC%95%84%EC%8B%A0%EC%95%84%EB%A7%88%EC%9D%B4%EB%93%9C+10%25+%EC%A7%95%ED%81%AC+1%25"
                else -> fallbackUrl
            }
            else -> fallbackUrl
        }
    }

    fun routineChecklistTitle(dateStr: String, lang: AppLang) = when (lang) {
        AppLang.EN -> "Action Routine Checklist ($dateStr)"
        AppLang.JA -> "今日のルーティンチェック ($dateStr)"
        AppLang.ZH -> "今日护肤打卡清单 ($dateStr)"
        AppLang.KO -> "오늘의 실천 루틴 체크리스트 ($dateStr)"
    }

    fun routineDoneCounter(done: Int, total: Int, lang: AppLang) = when (lang) {
        AppLang.EN -> "Done $done/$total today"
        AppLang.JA -> "本日 $done/$total 完了"
        AppLang.ZH -> "今日已完成 $done/$total"
        AppLang.KO -> "오늘 $done/$total 완료"
    }

    fun localizeRoutineStep(tag: String, status: String, fallbackText: String, lang: AppLang): String {
        if (lang == AppLang.EN) return fallbackText
        val isOptimal = status == "optimal_balance"
        return when (tag) {
            "AM" -> if (isOptimal) {
                when (lang) {
                    AppLang.JA -> "低刺激の洗顔を維持し、毎朝の日焼け止めを丁寧に塗布してください。"
                    AppLang.ZH -> "保持温和洁面，并在每天早晨涂抹广谱防晒霜。"
                    AppLang.KO -> "순한 아침 세안을 유지하고 자외선 차단제를 꼼꼼히 발라주세요."
                    else -> fallbackText
                }
            } else {
                when (lang) {
                    AppLang.JA -> "マイルドな洗顔後、気になる部位に集中ケア美容液を塗り、日焼け止めで仕上げます。"
                    AppLang.ZH -> "温和洁面后，在重点部位涂抹针对性精华，最后涂抹防晒霜。"
                    AppLang.KO -> "약산성 클렌저로 가볍게 세안한 뒤, 고민 부위에 맞춤 성분을 바르고 선크림으로 마무리하세요."
                    else -> fallbackText
                }
            }
            "PM" -> if (isOptimal) {
                when (lang) {
                    AppLang.JA -> "夜は軽い保湿クリームで水分バリアを整え、十分な睡眠をとってください。"
                    AppLang.ZH -> "夜间涂抹轻薄修护保湿霜，并保证充足睡眠。"
                    AppLang.KO -> "가벼운 나이트 보습제로 피부 장벽을 채우고 충분한 휴식을 취하세요."
                    else -> fallbackText
                }
            } else {
                when (lang) {
                    AppLang.JA -> "丁寧にクレンジングした後、ナイトモイスチャライザーで肌の水分バリアを補給して休みます。"
                    AppLang.ZH -> "彻底清洁面部后，使用夜间保湿霜补充肌肤水分屏障并尽早休息。"
                    AppLang.KO -> "꼼꼼히 세안한 뒤 나이트 보습제로 무너진 수분 장벽을 보충하고 숙면을 취하세요."
                    else -> fallbackText
                }
            }
            else -> if (isOptimal) {
                when (lang) {
                    AppLang.EN -> fallbackText
                    AppLang.JA -> "日中はこまめに水分を補給し、規則正しい睡眠リズムを保ちましょう。"
                    AppLang.ZH -> "全天保持充足饮水，并维持规律的作息习惯。"
                    AppLang.KO -> "하루 동안 물을 충분히 마시고 규칙적인 수면 리듬을 유지하세요."
                }
            } else {
                when (lang) {
                    AppLang.EN -> fallbackText
                    AppLang.JA -> "こまめな水分摂取と、肌回復のための質の高い睡眠を優先してください。"
                    AppLang.ZH -> "优先保证全天水分摄入与高质量的修护睡眠。"
                    AppLang.KO -> "꾸준한 수분 섭취와 피부 회복을 위한 숙면을 최우선으로 챙기세요."
                }
            }
        }
    }

    // Passport, Disclaimer & Footer Buttons
    fun passportTitle(open: Boolean, lang: AppLang) = when (lang) {
        AppLang.EN -> "Result Provenance Passport ${if (open) "▲" else "▼"}"
        AppLang.JA -> "データ出典パスポート ${if (open) "▲" else "▼"}"
        AppLang.ZH -> "数据来源透明护照 ${if (open) "▲" else "▼"}"
        AppLang.KO -> "데이터 출처 투명성 패스포트 ${if (open) "▲" else "▼"}"
    }

    fun passportSkinScores(lang: AppLang) = when (lang) {
        AppLang.EN -> "Skin scores & masks"
        AppLang.JA -> "肌スコア＆マスク画像"
        AppLang.ZH -> "肌肤分数与掩膜"
        AppLang.KO -> "피부 점수 및 마스크"
    }

    fun passportSleep(lang: AppLang) = when (lang) {
        AppLang.EN -> "Sleep session"
        AppLang.JA -> "睡眠セッション記録"
        AppLang.ZH -> "睡眠记录"
        AppLang.KO -> "수면 세션 기록"
    }

    fun passportIngredients(lang: AppLang) = when (lang) {
        AppLang.EN -> "Ingredient suggestions"
        AppLang.JA -> "推奨成分ルール"
        AppLang.ZH -> "推荐成分规则"
        AppLang.KO -> "추천 성분 매칭"
    }

    fun examplePrefix(lang: AppLang) = when (lang) {
        AppLang.EN -> "e.g."
        AppLang.JA -> "例:"
        AppLang.ZH -> "例如:"
        AppLang.KO -> "예시:"
    }

    fun passportMetaLine(timeStr: String, maskCount: Int, lang: AppLang) = when (lang) {
        AppLang.EN -> "Analyzed: $timeStr · Masks: $maskCount"
        AppLang.JA -> "測定日時: $timeStr · マスク数: $maskCount"
        AppLang.ZH -> "分析时间: $timeStr · 掩膜数: $maskCount"
        AppLang.KO -> "분석 시각: $timeStr · 수신 마스크: ${maskCount}종"
    }

    fun routineTagDisplay(tag: String, lang: AppLang) = when (tag) {
        "AM" -> when (lang) {
            AppLang.EN -> "AM"
            AppLang.JA -> "朝"
            AppLang.ZH -> "早间"
            AppLang.KO -> "아침"
        }
        "PM" -> when (lang) {
            AppLang.EN -> "PM"
            AppLang.JA -> "夜"
            AppLang.ZH -> "晚间"
            AppLang.KO -> "저녁"
        }
        else -> when (lang) {
            AppLang.EN -> "Life"
            AppLang.JA -> "生活"
            AppLang.ZH -> "作息"
            AppLang.KO -> "생활"
        }
    }

    fun passportPrivacy(lang: AppLang) = when (lang) {
        AppLang.EN -> "Privacy: Neither the app nor the local proxy writes face photos to local disk. Frames are processed in-memory and transmitted to YouCam Skin AI for analysis."
        AppLang.JA -> "プライバシー保護: 顔写真は端末やローカルプロキシのディスクに保存されず、メモリ上で処理されYouCam Skin AIへ送信・解析されます。"
        AppLang.ZH -> "隐私保护：应用与本地代理均不会将人脸照片写入本地磁盘，图像在内存中处理并发送至 YouCam Skin AI 进行分析。"
        AppLang.KO -> "개인정보 보호: 얼굴 사진은 기기나 로컬 프록시 디스크에 저장되지 않으며, 메모리에서 전처리 후 YouCam Skin AI로 전송·분석됩니다."
    }

    fun disclaimerText(lang: AppLang) = when (lang) {
        AppLang.EN -> "AuraMirror provides cosmetic wellness information only. It does not diagnose, treat, or prescribe. Results vary with lighting and camera conditions."
        AppLang.JA -> "AuraMirrorは美容・スキンケアの参考情報のみを提供し、医療診断や処方は行いません。照明や撮影環境により結果が変動する場合があります。"
        AppLang.ZH -> "AuraMirror 仅提供美容护肤参考信息，不构成医疗诊断或处方。结果可能受光线和拍摄条件影响。"
        AppLang.KO -> "AuraMirror는 화장품·스킨케어 웰니스 참고 정보만 제공하며 의학적 진단이나 처방을 대신하지 않습니다. 조명 및 촬영 환경에 따라 수치가 달라질 수 있습니다."
    }

    fun exportShareBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Export / Share Report"
        AppLang.JA -> "レポートを共有・出力"
        AppLang.ZH -> "导出 / 分享报告"
        AppLang.KO -> "리포트 내보내기 / 공유"
    }

    fun copyMarkdownBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Copy Markdown"
        AppLang.JA -> "全文コピー"
        AppLang.ZH -> "复制全文"
        AppLang.KO -> "전체 리포트 복사"
    }

    fun closeRescanBtn(lang: AppLang) = when (lang) {
        AppLang.EN -> "Close & Scan Again"
        AppLang.JA -> "閉じて再スキャン"
        AppLang.ZH -> "关闭并重新扫描"
        AppLang.KO -> "닫고 다시 촬영하기"
    }

    fun copiedToast(lang: AppLang) = when (lang) {
        AppLang.EN -> "Full report copied to clipboard"
        AppLang.JA -> "レポート全文をクリップボードにコピーしました"
        AppLang.ZH -> "完整报告已复制到剪贴板"
        AppLang.KO -> "전체 리포트가 클립보드에 복사되었습니다"
    }
}
