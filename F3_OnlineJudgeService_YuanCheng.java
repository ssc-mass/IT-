import java.time.LocalDateTime;
import java.util.*;

/**
 * F3 在线实训中心模块示例代码
 * 负责人：袁成
 * 功能：算法题目管理、代码提交、沙箱配置、评测状态机、CTF靶场入口管理。
 */
public class F3_OnlineJudgeService_YuanCheng {
    private final Map<String, Problem> problems = new LinkedHashMap<>();
    private final Map<String, Submission> submissions = new LinkedHashMap<>();
    private final Map<String, LabEnvironment> labs = new LinkedHashMap<>();
    private final SandboxPolicy sandboxPolicy = new SandboxPolicy(2, 128, 3000);

    public String addProblem(String title, String difficulty, String sampleInput, String sampleOutput) {
        if (isBlank(title) || isBlank(difficulty)) {
            throw new IllegalArgumentException("题目标题和难度不能为空");
        }
        Problem problem = new Problem();
        problem.id = "P" + String.format("%03d", problems.size() + 1);
        problem.title = title;
        problem.difficulty = difficulty;
        problem.sampleInput = sampleInput;
        problem.sampleOutput = sampleOutput;
        problem.timeLimitMs = 1000;
        problem.memoryLimitMb = 128;
        problems.put(problem.id, problem);
        return problem.id;
    }

    public String submitCode(String studentId, String problemId, String language, String code) {
        if (!problems.containsKey(problemId)) {
            throw new IllegalArgumentException("题目不存在，无法提交代码");
        }
        if (!isSupportedLanguage(language)) {
            throw new IllegalArgumentException("暂不支持该语言：" + language);
        }
        Submission submission = new Submission();
        submission.id = "S" + String.format("%04d", submissions.size() + 1);
        submission.studentId = studentId;
        submission.problemId = problemId;
        submission.language = language;
        submission.code = code;
        submission.status = "WAITING";
        submission.submitTime = LocalDateTime.now();
        submissions.put(submission.id, submission);
        return submission.id;
    }

    public JudgeResult judge(String submissionId) {
        Submission submission = submissions.get(submissionId);
        if (submission == null) {
            return JudgeResult.fail("SUBMISSION_NOT_FOUND", "提交记录不存在");
        }
        Problem problem = problems.get(submission.problemId);
        submission.status = "COMPILING";
        JudgeResult compileResult = compileInSandbox(submission);
        if (!compileResult.accepted) {
            submission.status = compileResult.status;
            return compileResult;
        }
        submission.status = "RUNNING";
        JudgeResult runResult = runInSandbox(problem, submission);
        submission.status = runResult.status;
        submission.runningTimeMs = runResult.runningTimeMs;
        submission.memoryUsedMb = runResult.memoryUsedMb;
        return runResult;
    }

    private JudgeResult compileInSandbox(Submission submission) {
        if (submission.code.contains("System.exit") || submission.code.contains("Runtime.getRuntime")) {
            return JudgeResult.fail("REJECTED", "代码包含高风险系统调用，已被沙箱策略拦截");
        }
        if (submission.code.trim().length() < 10) {
            return JudgeResult.fail("COMPILE_ERROR", "代码过短，无法通过编译检查");
        }
        return JudgeResult.success("COMPILE_OK", 0, 0, "编译通过");
    }

    private JudgeResult runInSandbox(Problem problem, Submission submission) {
        int estimatedTime = estimateRunningTime(submission.code);
        int estimatedMemory = estimateMemory(submission.code);
        if (estimatedTime > sandboxPolicy.maxRunningTimeMs || estimatedTime > problem.timeLimitMs) {
            return JudgeResult.fail("TLE", "程序运行超出时间限制", estimatedTime, estimatedMemory);
        }
        if (estimatedMemory > sandboxPolicy.maxMemoryMb || estimatedMemory > problem.memoryLimitMb) {
            return JudgeResult.fail("MLE", "程序运行超出内存限制", estimatedTime, estimatedMemory);
        }
        if (submission.code.contains(problem.sampleOutput)) {
            return JudgeResult.success("AC", estimatedTime, estimatedMemory, "答案正确");
        }
        if (submission.code.contains("while(true)")) {
            return JudgeResult.fail("TLE", "检测到疑似无限循环", estimatedTime + 3000, estimatedMemory);
        }
        return JudgeResult.fail("WA", "输出结果与标准答案不一致", estimatedTime, estimatedMemory);
    }

    private int estimateRunningTime(String code) {
        int loopWeight = countKeyword(code, "for") * 150 + countKeyword(code, "while") * 200;
        return Math.max(80, Math.min(5000, code.length() / 3 + loopWeight));
    }

    private int estimateMemory(String code) {
        int arrayWeight = countKeyword(code, "new int[") * 20 + countKeyword(code, "ArrayList") * 15;
        return Math.max(16, Math.min(512, code.length() / 80 + arrayWeight + 16));
    }

    private int countKeyword(String code, String keyword) {
        int count = 0;
        int index = code.indexOf(keyword);
        while (index >= 0) {
            count++;
            index = code.indexOf(keyword, index + keyword.length());
        }
        return count;
    }

    public String createLab(String name, String type, String dockerImage, String guideUrl) {
        LabEnvironment lab = new LabEnvironment();
        lab.id = "L" + String.format("%03d", labs.size() + 1);
        lab.name = name;
        lab.type = type;
        lab.dockerImage = dockerImage;
        lab.guideUrl = guideUrl;
        lab.status = "STOPPED";
        labs.put(lab.id, lab);
        return lab.id;
    }

    public boolean startLab(String labId) {
        LabEnvironment lab = labs.get(labId);
        if (lab == null) {
            return false;
        }
        lab.status = "RUNNING";
        lab.lastResetTime = LocalDateTime.now();
        return true;
    }

    public boolean resetLab(String labId) {
        LabEnvironment lab = labs.get(labId);
        if (lab == null) {
            return false;
        }
        lab.status = "RUNNING";
        lab.lastResetTime = LocalDateTime.now();
        return true;
    }

    private boolean isSupportedLanguage(String language) {
        return Arrays.asList("Java", "C++", "Python").contains(language);
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static void main(String[] args) {
        F3_OnlineJudgeService_YuanCheng service = new F3_OnlineJudgeService_YuanCheng();
        String problemId = service.addProblem("两数之和", "简单", "1 2", "3");
        String code = "public class Main { public static void main(String[] args){ System.out.println(3); } }";
        String submissionId = service.submitCode("23110507069", problemId, "Java", code);
        System.out.println("评测结果：" + service.judge(submissionId));
        String labId = service.createLab("SQL注入基础靶场", "Web安全", "ctf/sql-lab:latest", "docs/sql-guide.md");
        service.startLab(labId);
        System.out.println("靶场状态：" + service.labs.get(labId));
    }

    static class Problem {
        String id;
        String title;
        String difficulty;
        String sampleInput;
        String sampleOutput;
        int timeLimitMs;
        int memoryLimitMb;
    }

    static class Submission {
        String id;
        String studentId;
        String problemId;
        String language;
        String code;
        String status;
        int runningTimeMs;
        int memoryUsedMb;
        LocalDateTime submitTime;
    }

    static class SandboxPolicy {
        int cpuCoreLimit;
        int maxMemoryMb;
        int maxRunningTimeMs;

        SandboxPolicy(int cpuCoreLimit, int maxMemoryMb, int maxRunningTimeMs) {
            this.cpuCoreLimit = cpuCoreLimit;
            this.maxMemoryMb = maxMemoryMb;
            this.maxRunningTimeMs = maxRunningTimeMs;
        }
    }

    static class JudgeResult {
        boolean accepted;
        String status;
        int runningTimeMs;
        int memoryUsedMb;
        String message;

        static JudgeResult success(String status, int runningTimeMs, int memoryUsedMb, String message) {
            JudgeResult result = new JudgeResult();
            result.accepted = "AC".equals(status) || "COMPILE_OK".equals(status);
            result.status = status;
            result.runningTimeMs = runningTimeMs;
            result.memoryUsedMb = memoryUsedMb;
            result.message = message;
            return result;
        }

        static JudgeResult fail(String status, String message) {
            return fail(status, message, 0, 0);
        }

        static JudgeResult fail(String status, String message, int runningTimeMs, int memoryUsedMb) {
            JudgeResult result = new JudgeResult();
            result.accepted = false;
            result.status = status;
            result.message = message;
            result.runningTimeMs = runningTimeMs;
            result.memoryUsedMb = memoryUsedMb;
            return result;
        }

        public String toString() {
            return "JudgeResult{accepted=" + accepted + ", status='" + status + "', time=" + runningTimeMs + "ms, memory=" + memoryUsedMb + "MB, message='" + message + "'}";
        }
    }

    static class LabEnvironment {
        String id;
        String name;
        String type;
        String dockerImage;
        String guideUrl;
        String status;
        LocalDateTime lastResetTime;

        public String toString() {
            return "LabEnvironment{id='" + id + "', name='" + name + "', type='" + type + "', status='" + status + "'}";
        }
    }
}
