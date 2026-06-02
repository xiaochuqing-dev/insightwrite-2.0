package com.insightwrite.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class DeepSeekService {

    @Value("${insightwrite.deepseek.api-key}")
    private String apiKey;

    @Value("${insightwrite.deepseek.api-url}")
    private String apiUrl;

    @Value("${insightwrite.deepseek.model-pro}")
    private String modelPro;

    @Value("${insightwrite.deepseek.model-flash}")
    private String modelFlash;

    private final RestTemplate restTemplate;

    public DeepSeekService() {
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(300))
                .build();
    }

    // 五色 → 标注类型映射（从需改进到很出色的梯度）
    private static final Map<String, String> COLOR_TYPE_MAP = new LinkedHashMap<>();
    static {
        COLOR_TYPE_MAP.put("红色", "error");       // 严重错误
        COLOR_TYPE_MAP.put("黄色", "suggestion");  // 建议优化
        COLOR_TYPE_MAP.put("淡黄色", "suggestion");// 细微调整 → 合并到建议优化
        COLOR_TYPE_MAP.put("淡蓝色", "highlight"); // 表达亮点
        COLOR_TYPE_MAP.put("蓝色", "excellent");   // 非常出色
    }

    private static final Map<String, String> COLOR_HEX_MAP = new LinkedHashMap<>();
    static {
        COLOR_HEX_MAP.put("红色", "#dc2626");
        COLOR_HEX_MAP.put("黄色", "#d97706");
        COLOR_HEX_MAP.put("淡黄色", "#ca8a04");
        COLOR_HEX_MAP.put("淡蓝色", "#059669");
        COLOR_HEX_MAP.put("蓝色", "#2563eb");
    }

    // 分析结果 + 解析后的标注列表
    public static class AnalysisResult {
        public String fullText;          // 完整分析结果文本
        public List<Map<String, Object>> annotations; // 标注数组
    }

    // 主入口：分析一篇文章
    public AnalysisResult analyze(String essayText, String mode, String customRequirement) {
        return analyze(essayText, mode, customRequirement, "", true);
    }

    public AnalysisResult analyze(String essayText, String mode, String customRequirement, String topic, boolean generateEssay) {
        return analyze(essayText, mode, customRequirement, topic, generateEssay, Map.of());
    }

    public AnalysisResult analyze(String essayText, String mode, String customRequirement, String topic,
                                  boolean generateEssay, Map<String, Object> analysisPreferences) {
        // 第一步：主分析
        String model = ("deep".equals(mode)) ? modelPro : modelFlash;
        String systemPrompt = getSystemPrompt(mode);
        String userMessage = buildUserMessage(essayText, mode, customRequirement, topic, generateEssay, analysisPreferences);
        String fullResult = callApi(model, systemPrompt, userMessage);

        // 第二步：提取（或二次生成）原文标注
        List<Map<String, Object>> annotations = extractAnnotations(essayText, fullResult);

        AnalysisResult result = new AnalysisResult();
        result.fullText = fullResult;
        result.annotations = annotations;
        return result;
    }

    // 构建用户消息
    private String buildUserMessage(String essay, String mode, String customReq, String topic, boolean generateEssay) {
        return buildUserMessage(essay, mode, customReq, topic, generateEssay, Map.of());
    }

    private String buildUserMessage(String essay, String mode, String customReq, String topic,
                                    boolean generateEssay, Map<String, Object> analysisPreferences) {
        int wordCount = essay.split("\\s+").length;
        int minAnnotations = Math.max(4, Math.min(wordCount / 20, 18));

        StringBuilder sb = new StringBuilder();
        // 用户特殊要求放在最前面
        if (customReq != null && !customReq.isBlank()) {
            sb.append("【用户特殊要求：").append(customReq.strip()).append("】"
                    + "请在分析结果中以【用户特殊要求】为标题（用【】包裹）优先呈现这部分内容。\n\n");
        } else {
            sb.append("本次没有用户特殊要求，严禁输出【用户特殊要求】章节。\n\n");
        }

        sb.append("【严禁废话】直接开始分析内容，不要输出任何开场白、确认指令、"
                + "或类似\"好的，我们开始分析\"的句子。第一个字就是分析。\n\n");

        sb.append("在开始分析之前，请先用2-3句话快速判断这篇文章的整体水平属于哪个等级"
                + "——基础薄弱（语法错误多、词汇简单、句式单调）、基础不错（语法基本正确、"
                + "词汇较丰富、有基本的句式变化）还是专业出色（语法精准、词汇地道丰富、句式多变、"
                + "结构严谨、论证有力）。然后根据你判断的实际水平来调整分析的深度和语气。\n\n");

        String preferencePrompt = buildPreferencePrompt(analysisPreferences);
        if (!preferencePrompt.isBlank()) {
            sb.append(preferencePrompt).append("\n\n");
        }

        if (topic != null && !topic.isBlank()) {
            sb.append("【文本标题/作文题目：").append(topic.strip()).append("】如果这是用户作文，请分析是否切题；如果是范文或外刊，请结合标题理解主题。\n\n");
        }

        // 范文开关：basic 和 quality 模式可由用户选择是否生成范文
        if (!"deep".equals(mode)) {
            if (generateEssay) {
                sb.append("注意：本次分析需要生成参考范文。请务必输出【参考范文】。\n\n");
            } else {
                sb.append("注意：本次分析不需要生成参考范文。严禁出现【参考范文】标题或\"无\"等占位文字。\n\n");
            }
        }

        sb.append("【硬性要求】分析结束后必须输出一个以【原文标注指引】为标题的段落"
                + "（标题必须用【】包裹）。该段落中允许使用 | 作为标注分隔符，但禁止任何其他 "
                + "Markdown 符号（**、*、`、```、-、>、#等）。原文共约").append(wordCount).append("词，")
                .append("本次必须标注至少").append(minAnnotations).append("条。")
                .append("每条格式严格为：序号: \"原文短语\" | 颜色 | 说明。")
                .append("五种颜色构成从差到好的梯度，必须均衡覆盖：")
                .append("红色=严重错误（语法、拼写、用词明显错误，必须纠正），")
                .append("黄色=待优化（不够地道、表达生硬，可以改进），")
                .append("淡黄色=细微调整（基本正确但可微调），")
                .append("淡蓝色=表达亮点（用词准确、句式流畅，值得肯定），")
                .append("蓝色=非常出色（精辟地道、结构巧妙，堪称范本）。")
                .append("【输出顺序】严格：①开头总体评价（无标题）→②【总分】（如有）→")
                .append("③【参考范文】（如有）→④【用户特殊要求】（如有）→⑤分项小标题。")
                .append("没有的章节直接跳过，不要留空，不要写\"无\"、\"暂无\"等占位。严禁调换顺序。\n")
                .append("【标注密度要求】不要全文标注。写得平平无奇、既没错又没亮点的句子，")
                .append("不需要标注。正面评价（淡蓝色、蓝色）占总标注 30%-50%。")
                .append("标注按原文先后顺序排列。\n\n");

        if ("deep".equals(mode)) {
            sb.append("请深度精读以下高质量英语文本：\n\n").append(essay);
        } else {
            sb.append("请分析并改进以下英语写作文本：\n\n").append(essay);
        }
        return sb.toString();
    }

    private String buildPreferencePrompt(Map<String, Object> preferences) {
        if (preferences == null || preferences.isEmpty()) return "";

        String detailLevel = allow(String.valueOf(preferences.getOrDefault("detailLevel", "standard")),
                Set.of("concise", "standard", "detailed"), "standard");
        String focus = allow(String.valueOf(preferences.getOrDefault("focus", "balanced")),
                Set.of("balanced", "grammar", "logic", "style"), "balanced");

        List<String> rules = new ArrayList<>();
        rules.add("学习友好硬性要求：凡是输出英语词汇、短语、搭配、句式、高分表达、亮点表达、可模仿写法或参考范文中的重点表达，必须紧跟中文意思或中文解释；格式可写为 English phrase（中文意思）。不要只列英文不解释。");
        if ("concise".equals(detailLevel)) {
            rules.add("输出详略偏好：在不改变既有章节顺序和标题的前提下，每个问题点控制在1-2句，只保留结论、原因和必要改法；示例数量从严筛选，但不得省略【原文标注指引】、中文释义和关键修改建议。");
        } else if ("detailed".equals(detailLevel)) {
            rules.add("输出详略偏好：在不新增章节、不写开场白的前提下，每个重要问题补充原因、影响和可执行改法；亮点表达要说明好在哪里、适合什么语境，并附中文意思。");
        } else {
            rules.add("输出详略偏好：保持标准密度，解释不要空泛；每个英语表达摘录都要有中文意思，避免只给英文词组列表。");
        }

        switch (focus) {
            case "grammar" -> rules.add("关注重点偏好：在既有分析框架内更重视语法准确度、用词地道度和句式自然度；指出错误时优先给出正确写法和中文原因。");
            case "logic" -> rules.add("关注重点偏好：在既有分析框架内更重视段落推进、论证深度、衔接和中心观点清晰度；建议必须具体到段落或句子层面。");
            case "style" -> rules.add("关注重点偏好：在既有分析框架内更重视表达风格、语言节奏、高分表达和可模仿写法；所有高分表达必须附中文意思和使用价值。");
            default -> rules.add("关注重点偏好：保持均衡，不额外偏向单一维度；避免泛泛评价。");
        }

        return "【用户分析偏好】" + String.join("", rules)
                + "这些偏好只能调节侧重点和详略，严禁改变三种模式的输出顺序、标题规则、Markdown禁令、废话禁令和标注格式。";
    }

    private String allow(String value, Set<String> allowed, String fallback) {
        return allowed.contains(value) ? value : fallback;
    }

    // 根据模式获取 system prompt
    private String getSystemPrompt(String mode) {
        return switch (mode) {
            case "basic" -> buildBasicPrompt();
            case "quality" -> buildQualityPrompt();
            case "deep" -> buildDeepPrompt();
            default -> buildQualityPrompt();
        };
    }

    // 调用 DeepSeek API
    private String callApi(String model, String systemPrompt, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("temperature", 0.3);
        body.put("max_tokens", 7000);
        body.put("stream", false);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            Map<String, Object> respBody = response.getBody();
            if (respBody == null) throw new RuntimeException("API 返回为空");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("AI 分析失败: " + e.getMessage(), e);
        }
    }

    // 仅解析标注，不调 API（前端轮询时用）
    public List<Map<String, Object>> parseAnnotationsOnly(String essay, String resultText) {
        return extractAnnotations(essay, resultText);
    }

    // 解析【原文标注指引】段落 → annotations 数组
    private List<Map<String, Object>> extractAnnotations(String essay, String fullResult) {
        List<Map<String, Object>> list = new ArrayList<>();

        // 提取【原文标注指引】段落
        String annotationBlock = extractAnnotationBlock(fullResult);
        if (annotationBlock == null) {
            return list; // 没有标注段，返回空
        }

        // 逐行解析
        String[] lines = annotationBlock.split("\n");
        for (String line : lines) {
            line = line.strip();
            if (line.isEmpty()) continue;

            // 格式: 序号: "短语" | 颜色 | 说明
            int colonIdx = findAnnotationColon(line);
            if (colonIdx < 0) continue;

            String content = line.substring(colonIdx + 1).strip();
            String[] parts = content.split("\\|", 3);
            if (parts.length < 3) continue;

            String phrase = cleanQuotedPhrase(parts[0]);
            String color = parts[1].strip();
            String suggestion = parts[2].strip();
            if (phrase.isBlank()) continue;

            // 在原文中查找短语位置
            int pos = essay.indexOf(phrase);
            if (pos < 0) continue;

            String type = COLOR_TYPE_MAP.getOrDefault(color, "suggestion");
            String hex = COLOR_HEX_MAP.getOrDefault(color, "#666666");
            String severity = "medium";
            if ("红色".equals(color)) severity = "high";
            else if ("淡黄色".equals(color)) severity = "low";
            // 正面评价（亮点、出色）标记为 positive
            String tone = ("highlight".equals(type) || "excellent".equals(type)) ? "positive" : "constructive";

            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("start", pos);
            ann.put("end", pos + phrase.length());
            ann.put("type", type);
            ann.put("color", hex);
            ann.put("suggestion", suggestion);
            ann.put("severity", severity);
            ann.put("tone", tone);
            list.add(ann);
        }
        return list;
    }

    private String extractAnnotationBlock(String text) {
        String block = extractBlock(text, "【原文标注指引】");
        if (block != null) return block;

        for (String header : List.of("原文标注指引：", "原文标注指引:")) {
            int startIdx = text.indexOf(header);
            if (startIdx >= 0) {
                int contentStart = startIdx + header.length();
                int endIdx = text.length();
                int nextHeader = text.indexOf("【", contentStart);
                if (nextHeader >= 0) endIdx = nextHeader;
                return text.substring(contentStart, endIdx).strip();
            }
        }
        return null;
    }

    private int findAnnotationColon(String line) {
        int asciiColon = line.indexOf(':');
        int chineseColon = line.indexOf('：');
        if (asciiColon < 0) return chineseColon;
        if (chineseColon < 0) return asciiColon;
        return Math.min(asciiColon, chineseColon);
    }

    private String cleanQuotedPhrase(String rawPhrase) {
        return rawPhrase.strip().replaceAll("^[\"“”'‘’]+|[\"“”'‘’]+$", "");
    }

    // 从文本中提取以某个标题开头的段落
    private String extractBlock(String text, String header) {
        int startIdx = text.indexOf(header);
        if (startIdx < 0) return null;
        int contentStart = startIdx + header.length();

        // 找下一个【标题】作为结束标记
        int endIdx = text.length();
        int nextHeader = text.indexOf("【", contentStart);
        if (nextHeader >= 0) endIdx = nextHeader;

        return text.substring(contentStart, endIdx).strip();
    }

    // 从完整结果中移除【原文标注指引】段落并清理残余Markdown，得到干净的改写文本
    public String stripAnnotationBlock(String fullResult) {
        String text = fullResult;
        int idx = text.indexOf("【原文标注指引】");
        if (idx >= 0) {
            int endIdx = text.indexOf("【", idx + 1);
            if (endIdx > idx) {
                text = (text.substring(0, idx) + text.substring(endIdx)).strip();
            } else {
                text = text.substring(0, idx).strip();
            }
        }
        return cleanMarkdown(text);
    }

    // 清理AI偶尔残留的Markdown符号
    private String cleanMarkdown(String text) {
        return text
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")  // **加粗** → 加粗
            .replaceAll("\\*(.+?)\\*", "$1")          // *斜体* → 斜体
            .replaceAll("`(.+?)`", "$1")              // `代码` → 代码
            .replaceAll("(?m)^#{1,6}\\s+", "")        // 去行首 # 标题
            .replaceAll("(?m)^[>\\-]\\s+", "")        // 去行首 > 引用、- 列表
            .trim();
    }

    // ─── 三种 Prompt ───

    private String noMd() {
        return "【绝对禁止使用任何Markdown格式符号】，包括但不限于：**加粗**、*斜体*、`代码块`、"
                + "```围栏```、|表格|、-列表项、>引用、#标题。仅【原文标注指引】段落中允许 | 作为"
                + "标注分隔符，全文其余位置包括 | 在内一律禁止任何 Markdown 符号。所有小标题（含"
                + "【原文标注指引】）必须使用【】包裹，不允许用其他任何符号替代。这是硬性要求，"
                + "违反将导致前端解析失败。";
    }

    private String buildBasicPrompt() {
        return "你是一位拥有15年英语教学经验的资深外教，专门帮助基础阶段的学习者全方位提升英语写作水平。\n\n"
                + "【严禁废话】直接输出分析内容。禁止\"好的\"、\"我们开始\"、\"已按要求\"等开场白。\n\n"
                + "如果用户输入中包含了 `【用户特殊要求】`，你必须优先执行该指令。\n\n"
                + "用中文输出分析结果，严格按下述顺序组织内容。" + noMd() + "\n\n"
                + "【输出顺序】\n"
                + "① 开头总体评价（2-3句话，无标题包裹）\n"
                + "② 【总分：X/100】——百分制总分\n"
                + "③ 【参考范文】——如果要求生成，英语范文约原文1.5倍长度\n"
                + "④ 【用户特殊要求】——如果有\n"
                + "⑤ 【语法问题】、【词汇问题】、【句式结构】、【内容连贯性】、【观点清晰度】\n"
                + "⑥ 【写得好的地方】——挑2-3个亮点\n"
                + "⑦ 【总结】——最该优先改进的两个方面\n\n"
                + "分项说明：\n"
                + "- 【语法问题】：挑最典型3-5个语法错误，一句话指错+一句话修改。\n"
                + "- 【词汇问题】：标3-5个最突出用词问题，给1-2个替换词。\n"
                + "- 【句式结构】：指2-3个可优化句式，给具体改写示例。\n"
                + "- 【内容连贯性】：检查段落逻辑，挑1-2个明显跳跃。\n"
                + "- 【观点清晰度】：判断是否离题，概括每段中心。\n\n"
                + "原文标注指引：用【原文标注指引】作为标题，格式：标注序号: \"原文短语\" | 颜色 | 说明。"
                + "颜色梯度：红色(严重错误)→黄色(待优化)→淡黄色(细微调整)→淡蓝色(表达亮点)→蓝色(非常出色)。"
                + "不要全文标注，平平无奇的句子直接跳过。正面评价占30%-50%。按原文顺序排列。";
    }

    private String buildQualityPrompt() {
        return "你是一位拥有15年英语教学经验的资深外教，同时也是一位严格的英语写作评审专家。"
                + "你的用户英语基础已经很好，你的任务是用更高的标准帮他们把文章从\"好\"推到\"更好\"。\n\n"
                + "【严禁废话】直接输出分析内容。禁止\"好的\"、\"我们开始\"、\"已按要求\"等开场白。\n\n"
                + "用中文输出分析结果，严格按下述顺序组织内容。" + noMd() + "\n\n"
                + "【输出顺序】\n"
                + "① 开头总体评价（2-3句话，无标题包裹）\n"
                + "② 【总分：X/100】——百分制总分\n"
                + "③ 【参考范文】——如果要求生成，英语范文放在最前面\n"
                + "④ 【用户特殊要求】——如果有\n"
                + "⑤ 【语法精准度】、【词汇品质】、【句式变化】、【结构层次】、【论证深度】\n"
                + "⑥ 【亮点表达】——好词好句摘录\n"
                + "⑦ 【总结】\n\n"
                + "原文标注指引：用【原文标注指引】作为标题，格式：标注序号: \"原文短语\" | 颜色 | 说明。"
                + "颜色梯度：红色(严重错误)→黄色(待优化)→淡黄色(细微调整)→淡蓝色(表达亮点)→蓝色(非常出色)。"
                + "只标注有问题的和出彩的，平淡无奇的句子不标。正面评价占30%-50%。按原文顺序排列。";
    }

    private String buildDeepPrompt() {
        return "你是一位资深英语文学鉴赏家和英语教育专家。用户贴进来的是一篇高质量的外刊文章、范文或名篇，"
                + "不是用户自己写的作文。你的任务不是纠错或评价，而是把这篇佳作的精妙之处彻底拆解出来，"
                + "帮用户学习和吸收。\n\n"
                + "【严禁废话】直接输出分析内容。禁止\"好的\"、\"我们开始\"、\"已按要求\"等开场白。\n\n"
                + noMd() + " 用中文组织分析内容。\n\n"
                + "【输出顺序】\n"
                + "① 开头总体评价（2-3句话，无标题包裹）\n"
                + "② 【逐句翻译】——每句直接写中文翻译，不要重复原文英语。"
                + "格式：1. 中文译文\n2. 中文译文\n...\n"
                + "③ 【高分表达】——摘录高分短语和词汇，解释用法\n"
                + "④ 【长难句分析】——拆解复杂句子结构\n"
                + "⑤ 【写作技巧】——赏析写作手法和技巧\n"
                + "⑥ 【原文标注指引】——格式：标注序号: \"原文短语\" | 颜色 | 说明。"
                + "颜色梯度：红色(极少)→黄色(偶有)→淡黄色(细微)→淡蓝色(亮点)→蓝色(精妙)。"
                + "标注以正面为主——淡蓝色和蓝色占比不少于70%。"
                + "覆盖60%-80%的文字，留白让用户感受重点。按原文顺序排列。\n\n"
                + "语气：专业、深入、有洞察力，像一位真正懂行的鉴赏家在品鉴佳作。";
    }
}
