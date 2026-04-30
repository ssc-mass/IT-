import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * F4 项目过程管理模块示例代码
 * 负责人：岳青霖
 * 功能：协作空间、项目文档上传、代码库关联、里程碑设定、节点临期提醒。
 */
public class F4_ProjectProcessService_YueQinglin {
    private final Map<String, Workspace> workspaces = new LinkedHashMap<>();
    private final Map<String, ProjectFile> files = new LinkedHashMap<>();
    private final Map<String, Milestone> milestones = new LinkedHashMap<>();

    public String createWorkspace(String teamId, String teamName, Set<String> memberIds) {
        if (isBlank(teamId) || isBlank(teamName)) {
            throw new IllegalArgumentException("团队编号和团队名称不能为空");
        }
        Workspace workspace = new Workspace();
        workspace.id = "W" + String.format("%03d", workspaces.size() + 1);
        workspace.teamId = teamId;
        workspace.teamName = teamName;
        workspace.memberIds = new LinkedHashSet<>(memberIds);
        workspace.createTime = LocalDateTime.now();
        workspace.repositoryUrls = new ArrayList<>();
        workspaces.put(workspace.id, workspace);
        return workspace.id;
    }

    public String uploadFile(String workspaceId, String uploaderId, String fileName, long fileSizeMb, String fileType) {
        Workspace workspace = workspaces.get(workspaceId);
        if (workspace == null) {
            throw new IllegalArgumentException("协作空间不存在");
        }
        if (!workspace.memberIds.contains(uploaderId)) {
            throw new SecurityException("上传者不属于当前团队，无权上传文件");
        }
        if (fileSizeMb > 500) {
            throw new IllegalArgumentException("单个文件不能超过500MB");
        }
        ProjectFile file = new ProjectFile();
        file.id = "F" + String.format("%04d", files.size() + 1);
        file.workspaceId = workspaceId;
        file.uploaderId = uploaderId;
        file.fileName = fileName;
        file.fileSizeMb = fileSizeMb;
        file.fileType = fileType;
        file.version = 1;
        file.uploadTime = LocalDateTime.now();
        files.put(file.id, file);
        workspace.fileIds.add(file.id);
        return file.id;
    }

    public boolean updateFileVersion(String fileId, String uploaderId, long newSizeMb) {
        ProjectFile file = files.get(fileId);
        if (file == null) {
            return false;
        }
        Workspace workspace = workspaces.get(file.workspaceId);
        if (workspace == null || !workspace.memberIds.contains(uploaderId)) {
            return false;
        }
        file.version++;
        file.fileSizeMb = newSizeMb;
        file.uploadTime = LocalDateTime.now();
        file.uploaderId = uploaderId;
        return true;
    }

    public boolean bindRepository(String workspaceId, String gitUrl) {
        Workspace workspace = workspaces.get(workspaceId);
        if (workspace == null || !isValidGitUrl(gitUrl)) {
            return false;
        }
        workspace.repositoryUrls.add(gitUrl);
        return true;
    }

    private boolean isValidGitUrl(String gitUrl) {
        if (isBlank(gitUrl)) {
            return false;
        }
        return gitUrl.startsWith("https://github.com/")
                || gitUrl.startsWith("https://gitee.com/")
                || gitUrl.endsWith(".git");
    }

    public String createMilestone(String workspaceId, String title, LocalDate deadline, String ownerId) {
        Workspace workspace = workspaces.get(workspaceId);
        if (workspace == null) {
            throw new IllegalArgumentException("协作空间不存在");
        }
        if (!workspace.memberIds.contains(ownerId)) {
            throw new SecurityException("负责人不属于当前团队");
        }
        Milestone milestone = new Milestone();
        milestone.id = "M" + String.format("%04d", milestones.size() + 1);
        milestone.workspaceId = workspaceId;
        milestone.title = title;
        milestone.deadline = deadline;
        milestone.ownerId = ownerId;
        milestone.status = "TODO";
        milestones.put(milestone.id, milestone);
        workspace.milestoneIds.add(milestone.id);
        return milestone.id;
    }

    public boolean completeMilestone(String milestoneId) {
        Milestone milestone = milestones.get(milestoneId);
        if (milestone == null) {
            return false;
        }
        milestone.status = "DONE";
        milestone.finishTime = LocalDateTime.now();
        return true;
    }

    public List<Reminder> scanUpcomingReminders(LocalDate today, int daysBefore) {
        List<Reminder> reminders = new ArrayList<>();
        for (Milestone milestone : milestones.values()) {
            if ("DONE".equals(milestone.status)) {
                continue;
            }
            long remainDays = milestone.deadline.toEpochDay() - today.toEpochDay();
            if (remainDays >= 0 && remainDays <= daysBefore) {
                Reminder reminder = new Reminder();
                reminder.milestoneId = milestone.id;
                reminder.ownerId = milestone.ownerId;
                reminder.title = milestone.title;
                reminder.remainDays = remainDays;
                reminder.message = "里程碑【" + milestone.title + "】还有" + remainDays + "天截止，请及时处理。";
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    public List<ProjectFile> listWorkspaceFiles(String workspaceId, String requesterId) {
        Workspace workspace = workspaces.get(workspaceId);
        if (workspace == null || !workspace.memberIds.contains(requesterId)) {
            return Collections.emptyList();
        }
        return workspace.fileIds.stream()
                .map(files::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static void main(String[] args) {
        F4_ProjectProcessService_YueQinglin service = new F4_ProjectProcessService_YueQinglin();
        String workspaceId = service.createWorkspace("T001", "星火竞赛队", new LinkedHashSet<>(Arrays.asList("S001", "S002", "S003")));
        service.bindRepository(workspaceId, "https://gitee.com/group7/contest-platform.git");
        service.uploadFile(workspaceId, "S001", "开题报告.docx", 12, "DOCX");
        service.uploadFile(workspaceId, "S002", "演示视频.mp4", 120, "VIDEO");
        service.createMilestone(workspaceId, "省赛作品提交", LocalDate.now().plusDays(2), "S001");
        System.out.println("文件列表：" + service.listWorkspaceFiles(workspaceId, "S001"));
        System.out.println("临期提醒：" + service.scanUpcomingReminders(LocalDate.now(), 3));
    }

    static class Workspace {
        String id;
        String teamId;
        String teamName;
        Set<String> memberIds = new LinkedHashSet<>();
        List<String> repositoryUrls = new ArrayList<>();
        List<String> fileIds = new ArrayList<>();
        List<String> milestoneIds = new ArrayList<>();
        LocalDateTime createTime;
    }

    static class ProjectFile {
        String id;
        String workspaceId;
        String uploaderId;
        String fileName;
        long fileSizeMb;
        String fileType;
        int version;
        LocalDateTime uploadTime;

        public String toString() {
            return "ProjectFile{id='" + id + "', fileName='" + fileName + "', version=" + version + ", size=" + fileSizeMb + "MB}";
        }
    }

    static class Milestone {
        String id;
        String workspaceId;
        String title;
        LocalDate deadline;
        String ownerId;
        String status;
        LocalDateTime finishTime;
    }

    static class Reminder {
        String milestoneId;
        String ownerId;
        String title;
        long remainDays;
        String message;

        public String toString() {
            return "Reminder{milestoneId='" + milestoneId + "', ownerId='" + ownerId + "', message='" + message + "'}";
        }
    }
}
