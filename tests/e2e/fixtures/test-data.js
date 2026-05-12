module.exports = {
  baseURL: process.env.BASE_URL || 'http://localhost:5173',

  student: {
    username: 'test_student',
    password: 'Test123456',
    email: 'student@test.com',
    phone: '13800138000'
  },

  staff: {
    username: 'test_worker',
    password: 'Test123456',
    name: '测试维修工'
  },

  admin: {
    username: 'admin',
    password: 'admin123',
    name: '管理员'
  },

  repairOrder: {
    campus: '主校区',
    area: '3号楼',
    locationDetail: '502 宿舍',
    description: '空调不制冷，制冷模式下出风口吹出的是常温风',
    category: ['生活类', '宿舍用电'],
    urgency: 'medium',
    phone: '13800138000'
  }
}
