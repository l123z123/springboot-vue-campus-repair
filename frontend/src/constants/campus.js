export const campusOptions = [
  {
    value: 'EAST',
    label: '河东校区',
    children: [
      { value: 'DORM', label: '普宿舍' },
      { value: 'APART', label: '公寓' },
      { value: 'TEACH', label: '教学楼' },
      { value: 'LIB', label: '图书馆' }
    ]
  },
  {
    value: 'WEST',
    label: '河西校区',
    children: [
      { value: 'EAST_YARD', label: '东苑' },
      { value: 'SOUTH_YARD', label: '南苑' },
      { value: 'WEST_YARD', label: '西苑' },
      { value: 'NORTH_YARD', label: '北苑' }
    ]
  }
]

export const categoryOptions = [
  {
    label: '基础设施',
    value: 'INFRA',
    children: [
      { value: 'INFRA_TRACK', label: '田径场' },
      { value: 'INFRA_BASKETBALL', label: '篮球场' },
      { value: 'INFRA_FOOTBALL', label: '足球场' }
    ]
  },
  {
    label: '生活设施',
    value: 'LIFE',
    children: [
      { value: 'LIFE_DORM', label: '宿舍' },
      { value: 'LIFE_CANTEEN', label: '食堂' },
      { value: 'LIFE_WATER', label: '饮水机' },
      { value: 'LIFE_WASHER', label: '洗衣机' },
      { value: 'LIFE_AIRCON', label: '空调' },
      { value: 'LIFE_LIGHTING', label: '照明' }
    ]
  },
  {
    label: '教学设施',
    value: 'TEACHING',
    children: [
      { value: 'TEACHING_MULTIMEDIA', label: '教室多媒体' },
      { value: 'TEACHING_LAB_EQUIP', label: '实验室设备' }
    ]
  }
]

