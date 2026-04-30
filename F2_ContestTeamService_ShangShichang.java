import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * F2 赛事与组队模块示例代码
 * 负责人：商世畅
 * 功能：赛事发布、赛事检索、队友招募、技能标签匹配、入队审批。
 */
public class F2_ContestTeamService_ShangShichang {
    private final Map<String, Contest> contests = new LinkedHashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<String, Candidate> candidates = new LinkedHashMap<>();

    public String publishContest(String title, String level, String category, LocalDate deadline) {
        if (isBlank(title) || isBlank(level) || isBlank(category) || deadline == null) {
            throw new IllegalArgumentException("赛事标题、级别、类别和截止日期不能为空");
        }
        Contest contest = new Contest();
        contest.id = "C" + String.format("%03d", contests.size() + 1);
        contest.title = title;
        contest.level = level;
        contest.category = category;
        contest.deadline = deadline;
        contest.status = "PENDING_AUDIT";
        contests.put(contest.id, contest);
        return contest.id;
    }

    public boolean approveContest(String contestId, String auditor) {
        Contest contest = contests.get(contestId);
        if (contest == null || isBlank(auditor)) {
            return false;
        }
        contest.status = "PUBLISHED";
        contest.auditor = auditor;
        contest.auditTime = LocalDate.now();
        return true;
    }

    public List<Contest> searchContest(String level, String category, String keyword) {
        return contests.values().stream()
                .filter(c -> "PUBLISHED".equals(c.status))
                .filter(c -> isBlank(level) || c.level.equals(level))
                .filter(c -> isBlank(category) || c.category.equals(category))
                .filter(c -> isBlank(keyword) || c.title.contains(keyword))
                .collect(Collectors.toList());
    }

    public String createTeam(String contestId, String leaderId, String teamName, int maxSize, Set<String> requiredSkills) {
        if (!contests.containsKey(contestId)) {
            throw new IllegalArgumentException("赛事不存在，无法创建队伍");
        }
        if (maxSize < 2 || maxSize > 5) {
            throw new IllegalArgumentException("队伍人数应在2到5人之间");
        }
        Team team = new Team();
        team.id = "T" + String.format("%03d", teams.size() + 1);
        team.contestId = contestId;
        team.leaderId = leaderId;
        team.teamName = teamName;
        team.maxSize = maxSize;
        team.requiredSkills = new LinkedHashSet<>(requiredSkills);
        team.members.add(leaderId);
        team.status = "RECRUITING";
        teams.put(team.id, team);
        return team.id;
    }

    public void addCandidate(String studentId, String name, Set<String> skills, int freeHoursPerWeek) {
        Candidate candidate = new Candidate();
        candidate.studentId = studentId;
        candidate.name = name;
        candidate.skills = new LinkedHashSet<>(skills);
        candidate.freeHoursPerWeek = freeHoursPerWeek;
        candidate.joinedTeam = false;
        candidates.put(studentId, candidate);
    }

    public List<MatchResult> recommendCandidates(String teamId, int limit) {
        Team team = teams.get(teamId);
        if (team == null) {
            return Collections.emptyList();
        }
        return candidates.values().stream()
                .filter(candidate -> !candidate.joinedTeam)
                .filter(candidate -> !team.members.contains(candidate.studentId))
                .map(candidate -> calculateMatch(team, candidate))
                .sorted(Comparator.comparingDouble((MatchResult r) -> r.score).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private MatchResult calculateMatch(Team team, Candidate candidate) {
        Set<String> intersection = new LinkedHashSet<>(candidate.skills);
        intersection.retainAll(team.requiredSkills);
        double skillScore = team.requiredSkills.isEmpty() ? 0.0 : intersection.size() * 1.0 / team.requiredSkills.size();
        double timeScore = Math.min(candidate.freeHoursPerWeek / 15.0, 1.0);
        double finalScore = skillScore * 0.75 + timeScore * 0.25;
        MatchResult result = new MatchResult();
        result.studentId = candidate.studentId;
        result.name = candidate.name;
        result.matchedSkills = intersection;
        result.score = Math.round(finalScore * 10000) / 100.0;
        return result;
    }

    public boolean inviteCandidate(String teamId, String studentId) {
        Team team = teams.get(teamId);
        Candidate candidate = candidates.get(studentId);
        if (team == null || candidate == null || team.members.size() >= team.maxSize) {
            return false;
        }
        team.pendingInvitations.add(studentId);
        return true;
    }

    public boolean approveJoin(String teamId, String studentId) {
        Team team = teams.get(teamId);
        Candidate candidate = candidates.get(studentId);
        if (team == null || candidate == null) {
            return false;
        }
        if (!team.pendingInvitations.contains(studentId)) {
            return false;
        }
        if (team.members.size() >= team.maxSize) {
            team.status = "FULL";
            return false;
        }
        team.pendingInvitations.remove(studentId);
        team.members.add(studentId);
        candidate.joinedTeam = true;
        if (team.members.size() == team.maxSize) {
            team.status = "WAITING_TEACHER_APPROVAL";
        }
        return true;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static void main(String[] args) {
        F2_ContestTeamService_ShangShichang service = new F2_ContestTeamService_ShangShichang();
        String contestId = service.publishContest("中国大学生计算机设计大赛", "国家级", "软件开发", LocalDate.now().plusDays(30));
        service.approveContest(contestId, "管理员A");
        String teamId = service.createTeam(contestId, "S001", "星火竞赛队", 3, new LinkedHashSet<>(Arrays.asList("Java", "前端", "算法")));
        service.addCandidate("S002", "王同学", new LinkedHashSet<>(Arrays.asList("Java", "算法", "Python")), 12);
        service.addCandidate("S003", "刘同学", new LinkedHashSet<>(Arrays.asList("前端", "Vue", "UI")), 10);
        service.addCandidate("S004", "赵同学", new LinkedHashSet<>(Arrays.asList("文档", "演讲")), 5);
        System.out.println("已发布赛事：" + service.searchContest("国家级", "软件开发", "计算机"));
        System.out.println("推荐队友：" + service.recommendCandidates(teamId, 3));
        service.inviteCandidate(teamId, "S002");
        service.approveJoin(teamId, "S002");
        System.out.println("队伍状态：" + service.teams.get(teamId));
    }

    static class Contest {
        String id;
        String title;
        String level;
        String category;
        LocalDate deadline;
        String status;
        String auditor;
        LocalDate auditTime;

        public String toString() {
            return "Contest{id='" + id + "', title='" + title + "', level='" + level + "', category='" + category + "', status='" + status + "'}";
        }
    }

    static class Team {
        String id;
        String contestId;
        String leaderId;
        String teamName;
        int maxSize;
        String status;
        Set<String> requiredSkills = new LinkedHashSet<>();
        Set<String> members = new LinkedHashSet<>();
        Set<String> pendingInvitations = new LinkedHashSet<>();

        public String toString() {
            return "Team{id='" + id + "', teamName='" + teamName + "', members=" + members + ", status='" + status + "'}";
        }
    }

    static class Candidate {
        String studentId;
        String name;
        Set<String> skills;
        int freeHoursPerWeek;
        boolean joinedTeam;
    }

    static class MatchResult {
        String studentId;
        String name;
        Set<String> matchedSkills;
        double score;

        public String toString() {
            return "MatchResult{studentId='" + studentId + "', name='" + name + "', matchedSkills=" + matchedSkills + ", score=" + score + "}";
        }
    }
}
