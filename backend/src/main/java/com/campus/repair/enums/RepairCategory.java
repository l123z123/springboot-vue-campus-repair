package com.campus.repair.enums;

/**
 * 报修种类枚举
 */
public enum RepairCategory {

    // 基础设施
    INFRA_TRACK("INFRA_TRACK", "田径场", "INFRA"),
    INFRA_BASKETBALL("INFRA_BASKETBALL", "篮球场", "INFRA"),
    INFRA_FOOTBALL("INFRA_FOOTBALL", "足球场", "INFRA"),

    // 生活设施
    LIFE_DORM("LIFE_DORM", "宿舍", "LIFE"),
    LIFE_CANTEEN("LIFE_CANTEEN", "食堂", "LIFE"),
    LIFE_WATER("LIFE_WATER", "饮水机", "LIFE"),
    LIFE_WASHER("LIFE_WASHER", "洗衣机", "LIFE"),
    LIFE_AIRCON("LIFE_AIRCON", "空调", "LIFE"),
    LIFE_LIGHTING("LIFE_LIGHTING", "照明", "LIFE"),

    // 教学设施
    TEACHING_MULTIMEDIA("TEACHING_MULTIMEDIA", "教室多媒体", "TEACHING"),
    TEACHING_LAB_EQUIP("TEACHING_LAB_EQUIP", "实验室设备", "TEACHING");

    private final String code;
    private final String label;
    private final String group;

    RepairCategory(String code, String label, String group) {
        this.code = code;
        this.label = label;
        this.group = group;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getGroup() {
        return group;
    }
}

