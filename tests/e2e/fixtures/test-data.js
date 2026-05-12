module.exports = {
  baseURL: process.env.BASE_URL || 'http://localhost:5173',

  student: {
    username: '20260001',
    password: '123456',
    role: 0,
    email: 'student20260001@campusrepair.demo',
    phone: '13800138000'
  },

  staff: {
    username: 'worker01',
    password: '123456',
    role: 1,
    name: '陈立军'
  },

  admin: {
    username: 'admin',
    password: '123456',
    role: 2,
    name: '马文轩'
  },

  repairOrder: {
    campusLabel: '河东校区',
    areaLabel: '教学楼',
    locationDetail: '502教室',
    description: '空调不制冷，出风口吹出常温风',
    phone: '13800138000'
  }
}
