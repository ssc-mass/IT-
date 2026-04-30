import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * F1 用户与权限模块示例代码
 * 负责人：陈牧歌
 * 功能：学生/教师注册、系统登录、技术标签维护、角色权限判断。
 * 说明：本文件用于 IT 项目管理实验3“配置项代码管理”，代码不依赖数据库，使用内存集合模拟业务流程。
 */
public class F1_UserPermissionService_ChenMuge {
    private final Map<String, UserAccount> accounts = new LinkedHashMap<>();
    private final Map<String, Set<String>> rolePermissions = new LinkedHashMap<>();
    private final Pattern passwordPattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,18}$");

    public F1_UserPermissionService_ChenMuge() {
        rolePermissions.put("STUDENT", new LinkedHashSet<>(Arrays.asList(
                "contest:view", "team:join", "practice:submit", "profile:edit", "resume:export"
        )));
        rolePermissions.put("TEACHER", new LinkedHashSet<>(Arrays.asList(
                "contest:publish", "team:audit", "project:monitor", "milestone:review"
        )));
        rolePermissions.put("ADMIN", new LinkedHashSet<>(Arrays.asList(
                "user:manage", "contest:audit", "data:dashboard", "system:config"
        )));
    }

    public RegisterResult registerStudent(String username, String studentNo, String rawPassword) {
        if (isBlank(username) || isBlank(studentNo)) {
            return RegisterResult.fail("学生姓名或学号不能为空");
        }
        if (!studentNo.matches("\\d{8,12}")) {
            return RegisterResult.fail("学号格式不正确，应为8到12位数字");
        }
        return createAccount(username, studentNo, rawPassword, "STUDENT");
    }

    public RegisterResult registerTeacher(String username, String teacherNo, String rawPassword) {
        if (isBlank(username) || isBlank(teacherNo)) {
            return RegisterResult.fail("教师姓名或工号不能为空");
        }
        if (!teacherNo.matches("T?\\d{5,10}")) {
            return RegisterResult.fail("教师工号格式不正确");
        }
        return createAccount(username, teacherNo, rawPassword, "TEACHER");
    }

    private RegisterResult createAccount(String username, String accountNo, String rawPassword, String role) {
        if (accounts.containsKey(accountNo)) {
            return RegisterResult.fail("账号已存在，不能重复注册");
        }
        if (!isValidPassword(rawPassword)) {
            return RegisterResult.fail("密码必须包含字母和数字，长度6到18位");
        }
        String salt = UUID.randomUUID().toString().substring(0, 8);
        String hashedPassword = simpleHash(rawPassword + salt);
        UserAccount account = new UserAccount(accountNo, username, role, salt, hashedPassword);
        accounts.put(accountNo, account);
        return RegisterResult.success("注册成功", accountNo, role);
    }

    public LoginResult login(String accountNo, String rawPassword) {
        UserAccount account = accounts.get(accountNo);
        if (account == null) {
            return LoginResult.fail("账号不存在");
        }
        String inputHash = simpleHash(rawPassword + account.salt);
        if (!inputHash.equals(account.hashedPassword)) {
            account.failedCount++;
            return LoginResult.fail("密码错误，失败次数：" + account.failedCount);
        }
        account.failedCount = 0;
        account.lastLoginTime = LocalDateTime.now();
        String token = account.role + "-" + account.accountNo + "-" + UUID.randomUUID();
        return LoginResult.success("登录成功", token, account.role, new ArrayList<>(getPermissions(account.role)));
    }

    public boolean addSkillTag(String accountNo, String tag) {
        UserAccount account = accounts.get(accountNo);
        if (account == null || isBlank(tag)) {
            return false;
        }
        account.skillTags.add(tag.trim());
        return true;
    }

    public boolean removeSkillTag(String accountNo, String tag) {
        UserAccount account = accounts.get(accountNo);
        if (account == null) {
            return false;
        }
        return account.skillTags.remove(tag);
    }

    public List<String> listSkillTags(String accountNo) {
        UserAccount account = accounts.get(accountNo);
        if (account == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(account.skillTags);
    }

    public boolean hasPermission(String accountNo, String permission) {
        UserAccount account = accounts.get(accountNo);
        if (account == null) {
            return false;
        }
        return getPermissions(account.role).contains(permission);
    }

    private Set<String> getPermissions(String role) {
        return rolePermissions.getOrDefault(role, Collections.emptySet());
    }

    private boolean isValidPassword(String password) {
        return password != null && passwordPattern.matcher(password).matches();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private String simpleHash(String value) {
        return Integer.toHexString(Objects.hash(value, "contest-platform-salt"));
    }

    public static void main(String[] args) {
        F1_UserPermissionService_ChenMuge service = new F1_UserPermissionService_ChenMuge();
        System.out.println(service.registerStudent("张三", "23110507069", "abc123"));
        System.out.println(service.registerTeacher("李老师", "T10086", "teach123"));
        System.out.println(service.login("23110507069", "abc123"));
        service.addSkillTag("23110507069", "Java");
        service.addSkillTag("23110507069", "Python");
        System.out.println("学生技术标签：" + service.listSkillTags("23110507069"));
        System.out.println("是否能提交练习：" + service.hasPermission("23110507069", "practice:submit"));
    }

    static class UserAccount {
        String accountNo;
        String username;
        String role;
        String salt;
        String hashedPassword;
        int failedCount;
        LocalDateTime lastLoginTime;
        Set<String> skillTags = new LinkedHashSet<>();

        UserAccount(String accountNo, String username, String role, String salt, String hashedPassword) {
            this.accountNo = accountNo;
            this.username = username;
            this.role = role;
            this.salt = salt;
            this.hashedPassword = hashedPassword;
        }
    }

    static class RegisterResult {
        boolean success;
        String message;
        String accountNo;
        String role;

        static RegisterResult success(String message, String accountNo, String role) {
            RegisterResult result = new RegisterResult();
            result.success = true;
            result.message = message;
            result.accountNo = accountNo;
            result.role = role;
            return result;
        }

        static RegisterResult fail(String message) {
            RegisterResult result = new RegisterResult();
            result.success = false;
            result.message = message;
            return result;
        }

        public String toString() {
            return "RegisterResult{success=" + success + ", message='" + message + "', accountNo='" + accountNo + "', role='" + role + "'}";
        }
    }

    static class LoginResult {
        boolean success;
        String message;
        String token;
        String role;
        List<String> permissions;

        static LoginResult success(String message, String token, String role, List<String> permissions) {
            LoginResult result = new LoginResult();
            result.success = true;
            result.message = message;
            result.token = token;
            result.role = role;
            result.permissions = permissions;
            return result;
        }

        static LoginResult fail(String message) {
            LoginResult result = new LoginResult();
            result.success = false;
            result.message = message;
            result.permissions = Collections.emptyList();
            return result;
        }

        public String toString() {
            return "LoginResult{success=" + success + ", message='" + message + "', role='" + role + "', permissions=" + permissions + "}";
        }
    }
}
