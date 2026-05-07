# -*- coding: utf-8 -*-
# 运行: 在项目根目录执行  python scripts/gen_seed_50.py
import os

OUT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "sql", "seed_repair_orders_50.sql"))

# 50 条：覆盖管理员派单版状态机（0/1/3/4/5/6/7/8/9/10）。
G = [
    (90001, 0), (90002, 0), (90003, 0), (90004, 0), (90005, 0), (90006, 0),
    (90007, 1), (90008, 1), (90009, 1), (90010, 1), (90011, 1),
    (90012, 3), (90013, 3), (90014, 3), (90015, 3),
    (90016, 3), (90017, 3), (90018, 3),
    (90019, 4), (90020, 4), (90021, 4), (90022, 4),
    (90023, 5), (90024, 5), (90025, 5), (90026, 5), (90027, 5),
    (90028, 6), (90029, 6), (90030, 6),
    (90031, 7), (90032, 7),
    (90033, 8), (90034, 8), (90035, 8), (90036, 8), (90037, 8), (90038, 8), (90039, 8), (90040, 8),
    (90041, 9),
    (90042, 10),
    (90043, 4), (90044, 4), (90045, 4), (90046, 4),
    (90047, 5), (90048, 5), (90049, 5), (90050, 5),
]
ST_TITLE = {
    0: "待审核",
    1: "审核中",
    2: "已审核",
    3: "待派单",
    4: "已派工",
    5: "维修中",
    6: "维修完待学生确认",
    7: "已确认待评价",
    8: "已评价(闭环)",
    9: "已驳回",
    10: "已取消",
}

PL = [
    ("河东普宿2号", "EAST", "DORM", "LIFE_LIGHTING"),
    ("河西东苑3号", "WEST", "EAST_YARD", "LIFE_AIRCON"),
    ("河东一教A103", "EAST", "TEACH", "TEACHING_MULTIMEDIA"),
    ("河西西苑5号", "WEST", "WEST_YARD", "LIFE_WATER"),
    ("河东公寓7号", "EAST", "APART", "LIFE_DORM"),
    ("河西南苑", "WEST", "SOUTH_YARD", "LIFE_CANTEEN"),
    ("河东逸夫图3F", "EAST", "LIB", "LIFE_LIGHTING"),
    ("河西北苑", "WEST", "NORTH_YARD", "LIFE_DORM"),
    ("河东普宿8号", "EAST", "DORM", "LIFE_WASHER"),
    ("河西东苑1号", "WEST", "EAST_YARD", "LIFE_DORM"),
]
ISS = [
    "日光灯频闪", "空调滴水", "网口松动", "龙头滴漏", "吸顶不亮", "门把松动", "投影色偏", "插座无电",
    "地漏慢", "空开跳闸", "外机异响", "窗玻璃裂", "开水不热", "电梯灯坏", "地面积水", "窗帘脱轨", "排风噪音", "反味",
    "阳台渗水", "应急灯不亮", "中控失灵", "课桌脱钉", "消栓门形变", "弱电异味", "排水返味", "墙皮起壳", "关窗不严", "排风扇停",
    "电表异响", "水温不稳", "地插松", "通道全黑", "车棚灯坏", "路面积水", "触屏不亮", "打纸机卡", "门感应失", "支架松",
    "插排跳闸", "限电误报", "廊灯不亮", "梯扶松", "消防灯弱", "与旧单重", "假期不用", "多盏待换", "已派等上门", "公区糊味",
    "教室钟停", "广播杂音", "路边灯", "教屏闪", "侧阀漏水", "洗衣地湿", "声控失", "不制冷", "网课常掉", "公区合单",
    "教桌破损", "黑板灯", "楼道积水", "快递柜", "公区门吸", "墙面插座", "走廊广播", "楼梯灯", "车位灯", "球场灯"
]
# 与 @s1..@s5 一一对应；五人真实姓名互不相同，与 init_data / init_all / fix 一致
NAMES = [
    "周子涵(20260001·计算机学院)",
    "张浩(计算机学院)",
    "李思琪(外国语学院)",
    "王梓晨(机电工程学院)",
    "赵心怡(经济管理学院)",
]


def esc(s):
    return s.replace("'", "''")


def d(day, h=0):
    return f"DATE_SUB(@t0, INTERVAL {day} DAY) + INTERVAL {h} HOUR"


def stud(oid):
    m = oid % 5
    return f"@s{m if m else 5}"


def nidx(oid):
    m = oid % 5
    return (m - 1) % 5 if m else 4


def work(oid):
    return f"@w{(oid % 4) + 1}"


def six(st, oid):
    b = 8 + (oid % 20)
    if st == 0:
        return "NULL,NULL,NULL,NULL,NULL,NULL"
    if st == 1:
        return f"{d(b)},NULL,NULL,NULL,NULL,NULL"
    if st == 2:
        return f"{d(b+1)},NULL,NULL,NULL,NULL,NULL"
    if st == 3:
        return f"{d(b+2)},NULL,NULL,NULL,NULL,NULL"
    if st == 4:
        return f"{d(b+2)},{d(b+1)},NULL,NULL,NULL,NULL"
    if st == 5:
        return f"{d(b+3)},{d(b+2)},{d(b+1)},NULL,NULL,NULL"
    if st == 6:
        return f"{d(b+3)},{d(b+2)},{d(b+1)},NULL,{d(b)},NULL"
    if st == 7:
        return f"{d(b+4)},{d(b+3)},{d(b+2)},{d(b)},{d(b)},NULL"
    if st == 8:
        return f"{d(b+4)},{d(b+3)},{d(b+2)},{d(b+1)},{d(b)},NULL"
    if st == 9:
        return f"{d(b)},NULL,NULL,NULL,NULL,NULL"
    if st == 10:
        return f"{d(b+1)},NULL,NULL,NULL,NULL,NULL"
    return "NULL,NULL,NULL,NULL,NULL,NULL"


def need_w(st):
    return st in (4, 5, 6, 7, 8)


def build_rows():
    lines = []
    for (oid, st) in G:
        k = oid - 90001
        cname, cps, are, cat = PL[k % len(PL)]
        iss = ISS[k % len(ISS)]
        stu = stud(oid)
        w = work(oid) if need_w(st) else "NULL"
        u = 3 if oid in (90016, 90017) else (2 if st in (2, 3, 4, 5, 6) and (k % 2 == 0) else 1)
        isu = 1 if oid == 90016 else 0
        t6 = six(st, oid)
        nm = NAMES[nidx(oid)]
        ctime = d(3 + (oid % 20), (oid * 2) % 5)
        ut = d(1, (oid % 8))
        if st in (0, 1):
            ctime = d(2 + (oid % 4))
        if st == 0:
            ctime = d(1 + (oid % 2))
        st_lab = ST_TITLE[st]
        room = 300 + (oid % 50)
        title = f"【{st_lab}】{cname}·{iss}·{room}室"
        if st == 9:
            desc = f"报修人：{nm}。{iss}。审核意见：与既有工单重复或材料不全，请合并处理。"
        elif st == 10:
            desc = f"报修人：{nm}。{iss}。因离校/自修，学生申请撤销本次报修。"
        else:
            desc = (
                f"报修人：{nm}。情况：{cname}现场出现「{iss}」。"
                f"已影响日常，希望后勤协调。可预约上门时间：工作日下午。联系电话同系统预留。"
            )
        loc = f"{'河西校区' if cps == 'WEST' else '河东校区'}·{cname}·{room}室"
        p = "139%02d%08d" % ((k % 90) + 9, (oid * 7 + k) % 100000000)
        if len(p) < 11:
            p = f"1390000{k % 10000:04d}"
        p = p[:11]
        rmk = f"seed#50 st={st}"
        line = "(%d,%s,%s,'%s','%s','%s','%s','%s','%s','%s',%d,%d,@img,'%s',%d,%s,0,0,%s,%s)" % (
            oid, stu, w, esc(title), esc(desc), esc(loc), p, cps, are, cat, u, st, esc(rmk), isu, t6, ctime, ut
        )
        lines.append(line)
    return lines


def main():
    head = r"""-- =============================================================================
-- 50 条样例工单 + 8 条评价(对应 8 条已评价单) + 12 条聊天
-- 状态：管理员派单版 (0待审核/1审核中/3待派单/4已派单/5维修中/6维修完成/7学生确认/8已评价/9已拒绝/10已取消)
-- 前置: 必须先有 sys_user 数据。执行 backend/.../init_data.sql，建议再 init_all_accounts.sql
-- 导入(Windows 建议): chcp 65001
--        或: mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u root -p campus_repair < sql/seed_repair_orders_50.sql
-- 主键: order 90001-90050, eval 950001-950008(对应 status=8 的 8 单), chat 960001-960012
-- =============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;
USE campus_repair;

-- 学号 @s1..@s5：查不到时回退到「任意学生」再回退 1，避免 user_id 为 NULL 导致 1048 错误
SET @s1 := COALESCE(
  (SELECT user_id FROM sys_user WHERE username = '20260001' LIMIT 1),
  (SELECT user_id FROM sys_user WHERE role = 0 AND is_deleted = 0 ORDER BY user_id ASC LIMIT 1),
  1
);
SET @s2 := COALESCE((SELECT user_id FROM sys_user WHERE username = 'stu001' LIMIT 1), @s1);
SET @s3 := COALESCE((SELECT user_id FROM sys_user WHERE username = 'stu002' LIMIT 1), @s1);
SET @s4 := COALESCE((SELECT user_id FROM sys_user WHERE username = 'stu003' LIMIT 1), @s1);
SET @s5 := COALESCE((SELECT user_id FROM sys_user WHERE username = 'stu004' LIMIT 1), @s1);
-- 维修工：支持 worker01 与 repair01，再回退任意维修工/2
SET @w1 := COALESCE(
  (SELECT user_id FROM sys_user WHERE username = 'worker01' LIMIT 1),
  (SELECT user_id FROM sys_user WHERE username = 'repair01' LIMIT 1),
  (SELECT user_id FROM sys_user WHERE role = 1 AND is_deleted = 0 ORDER BY user_id ASC LIMIT 1),
  2
);
SET @w2 := COALESCE((SELECT user_id FROM sys_user WHERE username = 'repair02' LIMIT 1), @w1);
SET @w3 := COALESCE((SELECT user_id FROM sys_user WHERE username = 'repair03' LIMIT 1), @w1);
SET @w4 := COALESCE((SELECT user_id FROM sys_user WHERE username = 'repair04' LIMIT 1), @w1);
SET @t0 := NOW();
SET @ph := '13900000000';
SET @img := '[]';

DELETE FROM chat_message WHERE (id BETWEEN 960001 AND 960999) OR (order_id BETWEEN 90001 AND 90050);
DELETE FROM repair_evaluation WHERE (eval_id BETWEEN 950001 AND 950099) OR (order_id BETWEEN 90001 AND 90050);
DELETE FROM repair_order WHERE order_id BETWEEN 90001 AND 90050;

INSERT INTO `repair_order` (
  `order_id`, `user_id`, `repairman_id`, `title`, `description`, `location`, `phone_number`, `campus`, `area`, `category`, `urgency`, `status`, `images`, `remark`, `is_urgent`,
  `audit_time`, `dispatch_time`, `start_time`, `confirm_time`, `completed_time`, `completed_images`, `version`, `is_deleted`, `create_time`, `update_time`
) VALUES
"""
    body = ",\n".join(build_rows()) + ";\n\n"

    # 8 条已评价工单 order_id: 90033-90040
    es = [
        (5, "响应快，修得稳，给五星。"), (4, "满意，等了一天但可以接受。"), (5, "师傅人好，问题根治。"),
        (3, "修好了但多跑了一趟。"), (4, "总体不错。"), (5, "后勤给力。"), (4, "会推荐给同学。"), (3, "勉强及格，总算好了。"),
    ]
    closed_ids = list(range(90033, 90041))
    ev = ",\n".join(
        "(%d,%d,%d,'%s',0,0,@t0,@t0)" % (950001 + i, closed_ids[i], es[i][0], esc(es[i][1])) for i in range(8)
    )
    ev_sql = f"INSERT INTO `repair_evaluation` (`eval_id`, `order_id`, `score`, `comment`, `is_anonymous`, `is_deleted`, `create_time`, `update_time`) VALUES\n{ev};\n\n"

    # stud(oid) 用于聊天双方
    chat_lines = [
        f"(960001,90019,{stud(90019)},{work(19)},'{esc('下午有课，能傍晚来吗？')}',NULL,1," + d(2, 0) + ")",
        f"(960002,90019,{work(19)},{stud(90019)},'{esc('可，我17:30到门厅')}',NULL,1," + d(1, 0) + ")",
        f"(960003,90020,{stud(90020)},{work(20)},'{esc('会响动大吗？')}',NULL,0," + d(3, 0) + ")",
        f"(960004,90020,{work(20)},{stud(90020)},'{esc('会轻点操作')}',NULL,1," + d(2, 0) + ")",
        f"(960005,90030,{stud(90030)},{work(30)},'{esc('修好了，感谢！')}',NULL,1," + d(0, 0) + ")",
        f"(960006,90032,{stud(90032)},{work(32)},'{esc('观察两天再写评价')}',NULL,1," + d(0, 0) + ")",
        f"(960007,90032,{work(32)},{stud(90032)},'{esc('有异常再留言')}',NULL,1," + d(0, 0) + ")",
        f"(960008,90047,{stud(90047)},{work(47)},'{esc('明早九点方便吗？')}',NULL,0," + d(0, 0) + ")",
        f"(960009,90047,{work(47)},{stud(90047)},'{esc('在宿舍，留门')}',NULL,0," + d(0, 0) + ")",
        f"(960010,90022,{stud(90022)},{work(22)},'{esc('中午教室无人可进场')}',NULL,1," + d(4, 0) + ")",
        f"(960011,90022,{work(22)},{stud(90022)},'{esc('好，我12点去')}',NULL,1," + d(3, 0) + ")",
        f"(960012,90023,{stud(90023)},{work(23)},'{esc('人已在现场旁，勿跑空')}',NULL,1," + d(0, 0) + ")",
    ]
    ch = "INSERT INTO `chat_message` (`id`, `order_id`, `sender_id`, `receiver_id`, `content`, `images`, `is_read`, `create_time`) VALUES\n"
    ch += ",\n".join(chat_lines) + ";\n"
    tail = "\nSET FOREIGN_KEY_CHECKS=1;\nSELECT 'sql/seed_repair_orders_50 已执行' AS message;\n"

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write(head + body + ev_sql + ch + tail)
    print("Wrote", OUT)


if __name__ == "__main__":
    main()
