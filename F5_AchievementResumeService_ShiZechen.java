import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * F5 成果与履历模块示例代码
 * 负责人：施则臣
 * 功能：获奖等级登记、学术论文记录、履历模板选择、一键生成文本版简历。
 */
public class F5_AchievementResumeService_ShiZechen {
    private final Map<String, StudentProfile> profiles = new LinkedHashMap<>();
    private final Map<String, AwardRecord> awards = new LinkedHashMap<>();
    private final Map<String, PaperRecord> papers = new LinkedHashMap<>();

    public void createProfile(String studentId, String name, String major, String email) {
        if (isBlank(studentId) || isBlank(name)) {
            throw new IllegalArgumentException("学生编号和姓名不能为空");
        }
        StudentProfile profile = new StudentProfile();
        profile.studentId = studentId;
        profile.name = name;
        profile.major = major;
        profile.email = email;
        profile.skills = new LinkedHashSet<>();
        profiles.put(studentId, profile);
    }

    public boolean addSkill(String studentId, String skill) {
        StudentProfile profile = profiles.get(studentId);
        if (profile == null || isBlank(skill)) {
            return false;
        }
        profile.skills.add(skill.trim());
        return true;
    }

    public String addAward(String studentId, String contestName, String level, String certificateUrl, LocalDate awardDate) {
        if (!profiles.containsKey(studentId)) {
            throw new IllegalArgumentException("学生档案不存在");
        }
        AwardRecord award = new AwardRecord();
        award.id = "A" + String.format("%04d", awards.size() + 1);
        award.studentId = studentId;
        award.contestName = contestName;
        award.level = level;
        award.certificateUrl = certificateUrl;
        award.awardDate = awardDate;
        award.verified = false;
        awards.put(award.id, award);
        return award.id;
    }

    public boolean verifyAward(String awardId, String reviewer) {
        AwardRecord award = awards.get(awardId);
        if (award == null || isBlank(reviewer)) {
            return false;
        }
        award.verified = true;
        award.reviewer = reviewer;
        return true;
    }

    public String addPaper(String studentId, String title, String venue, String authorRole, String contribution) {
        if (!profiles.containsKey(studentId)) {
            throw new IllegalArgumentException("学生档案不存在");
        }
        PaperRecord paper = new PaperRecord();
        paper.id = "R" + String.format("%04d", papers.size() + 1);
        paper.studentId = studentId;
        paper.title = title;
        paper.venue = venue;
        paper.authorRole = authorRole;
        paper.contribution = contribution;
        papers.put(paper.id, paper);
        return paper.id;
    }

    public String generateResume(String studentId, ResumeTemplate template) {
        StudentProfile profile = profiles.get(studentId);
        if (profile == null) {
            return "学生档案不存在，无法生成履历。";
        }
        List<AwardRecord> studentAwards = awards.values().stream()
                .filter(a -> a.studentId.equals(studentId))
                .sorted(Comparator.comparing((AwardRecord a) -> a.awardDate).reversed())
                .collect(Collectors.toList());
        List<PaperRecord> studentPapers = papers.values().stream()
                .filter(p -> p.studentId.equals(studentId))
                .collect(Collectors.toList());
        StringBuilder builder = new StringBuilder();
        appendHeader(builder, profile, template);
        appendSkills(builder, profile);
        appendAwards(builder, studentAwards);
        appendPapers(builder, studentPapers);
        appendFooter(builder, template);
        return builder.toString();
    }

    private void appendHeader(StringBuilder builder, StudentProfile profile, ResumeTemplate template) {
        if (template == ResumeTemplate.ACADEMIC) {
            builder.append("================ 学术竞赛履历 ================\n");
        } else {
            builder.append("================ 求职竞赛简历 ================\n");
        }
        builder.append("姓名：").append(profile.name).append("\n");
        builder.append("专业：").append(profile.major).append("\n");
        builder.append("邮箱：").append(profile.email).append("\n");
        builder.append("学号：").append(profile.studentId).append("\n\n");
    }

    private void appendSkills(StringBuilder builder, StudentProfile profile) {
        builder.append("【技术能力】\n");
        if (profile.skills.isEmpty()) {
            builder.append("暂无技术标签。\n\n");
            return;
        }
        int index = 1;
        for (String skill : profile.skills) {
            builder.append(index++).append(". ").append(skill).append("\n");
        }
        builder.append("\n");
    }

    private void appendAwards(StringBuilder builder, List<AwardRecord> studentAwards) {
        builder.append("【竞赛获奖】\n");
        if (studentAwards.isEmpty()) {
            builder.append("暂无竞赛获奖记录。\n\n");
            return;
        }
        for (AwardRecord award : studentAwards) {
            builder.append("- ").append(award.contestName)
                    .append("，").append(award.level)
                    .append("，获奖时间：").append(award.awardDate)
                    .append("，审核状态：").append(award.verified ? "已审核" : "待审核")
                    .append("\n");
        }
        builder.append("\n");
    }

    private void appendPapers(StringBuilder builder, List<PaperRecord> studentPapers) {
        builder.append("【学术论文】\n");
        if (studentPapers.isEmpty()) {
            builder.append("暂无论文记录。\n\n");
            return;
        }
        for (PaperRecord paper : studentPapers) {
            builder.append("- 题目：").append(paper.title).append("\n")
                    .append("  会议/期刊：").append(paper.venue).append("\n")
                    .append("  作者角色：").append(paper.authorRole).append("\n")
                    .append("  个人贡献：").append(paper.contribution).append("\n");
        }
        builder.append("\n");
    }

    private void appendFooter(StringBuilder builder, ResumeTemplate template) {
        if (template == ResumeTemplate.ACADEMIC) {
            builder.append("说明：本履历突出科研经历、竞赛成果与项目贡献，可用于推免和科研面试。\n");
        } else {
            builder.append("说明：本简历突出工程能力、项目经验与可量化成果，可用于企业求职。\n");
        }
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static void main(String[] args) {
        F5_AchievementResumeService_ShiZechen service = new F5_AchievementResumeService_ShiZechen();
        service.createProfile("23110507069", "袁成", "计算机科学与技术", "student@example.com");
        service.addSkill("23110507069", "Java后端开发");
        service.addSkill("23110507069", "算法竞赛训练");
        service.addSkill("23110507069", "Docker部署");
        String awardId = service.addAward("23110507069", "蓝桥杯全国软件和信息技术专业人才大赛", "省级一等奖", "cert/lanqiao.pdf", LocalDate.now().minusMonths(2));
        service.verifyAward(awardId, "指导教师");
        service.addPaper("23110507069", "面向竞赛训练平台的在线评测系统设计", "校级本科生科研训练项目", "第一作者", "负责系统设计、评测流程实现与实验测试");
        System.out.println(service.generateResume("23110507069", ResumeTemplate.ACADEMIC));
    }

    enum ResumeTemplate {
        ACADEMIC,
        INTERNET_JOB
    }

    static class StudentProfile {
        String studentId;
        String name;
        String major;
        String email;
        Set<String> skills;
    }

    static class AwardRecord {
        String id;
        String studentId;
        String contestName;
        String level;
        String certificateUrl;
        LocalDate awardDate;
        boolean verified;
        String reviewer;
    }

    static class PaperRecord {
        String id;
        String studentId;
        String title;
        String venue;
        String authorRole;
        String contribution;
    }
}
