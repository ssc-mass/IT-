# 7组 实验3 Git分支与代码配置项

## 分支与文件对应关系

| 姓名 | 负责模块 | 分支名称 | 分支下源文件 |
|---|---|---|---|
| 陈牧歌 | F1 用户与权限模块 | feature/F1-user-permission-chenmuge | src/main/java/com/contest/platform/F1_UserPermissionService_ChenMuge.java |
| 商世畅 | F2 赛事与组队模块 | feature/F2-contest-team-shangshichang | src/main/java/com/contest/platform/F2_ContestTeamService_ShangShichang.java |
| 袁成 | F3 在线实训中心模块 | feature/F3-online-judge-yuancheng | src/main/java/com/contest/platform/F3_OnlineJudgeService_YuanCheng.java |
| 岳青霖 | F4 项目过程管理模块 | feature/F4-project-process-yueqinglin | src/main/java/com/contest/platform/F4_ProjectProcessService_YueQinglin.java |
| 施则臣 | F5 成果与履历模块 | feature/F5-achievement-resume-shizechen | src/main/java/com/contest/platform/F5_AchievementResumeService_ShiZechen.java |

## 推荐提交命令示例

```bash
git checkout -b feature/F3-online-judge-yuancheng
git add src/main/java/com/contest/platform/F3_OnlineJudgeService_YuanCheng.java
git commit -m "feat(F3): add online judge service by YuanCheng"
git push origin feature/F3-online-judge-yuancheng
```
