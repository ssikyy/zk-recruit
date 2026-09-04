/**
 * 首页内容（需求文档 §7.2）。
 *
 * 第一版不提供后台内容管理，首页文案、企业数据与配图在此维护，随前端构建发布。
 * 图片放在 public/site/ 下，缺图的位置留 null，页面按 §17.2 的占位规则渲染。
 */
export const HOME_CONTENT = {
  hero: {
    slogan1: 'AI认知让空间更智能，让世界更美好',
    slogan2: '逐梦AI，与熵基一起成为 Change-Maker',
    bgImage: '/site/company_profile_bg.jpg',
    socialBtnText: '探索社会招聘',
    campusBtnText: '加入校园招聘'
  },

  about: {
    title: '认识熵基科技',
    intro:
      '熵基科技成立于2007年，是全球AI认知驱动型智能空间进化服务商。依托自主研发的多模态BioCV（计算机视觉与生物识别）与AI认知空间计算技术，熵基科技构建全要素智能感知体系，驱动空间从静态管理向自主决策与持续进化转型。',
    image: '/site/company_profile_bg.jpg',
    stats: [
      { label: '全球员工', value: '3400+', unit: '人' },
      { label: '研发人员', value: '950+', unit: '人' },
      { label: '分支机构', value: '100+', unit: '个' },
      { label: '国家和地区业务覆盖', value: '100+', unit: '个' }
    ],
    dataNote: '数据截至2025年12月31日'
  },

  business: {
    title: '四大业务方向',
    items: [
      {
        name: '智慧空间',
        desc: '以AI认知空间计算和多维感知技术连接人、车、物与环境，助力通行优化、能源管理、数据洞察和空间自主决策。',
        jobDirections: null,
        image: '/site/smart_space.jpg'
      },
      {
        name: '数字身份认证',
        desc: '融合多模态BioCV、大模型与区块链技术，提供精准、安全、便捷的数字身份认证能力，服务政务、金融、交通、教育、医疗等场景。',
        jobDirections: null,
        image: '/site/smart_recognition.jpg'
      },
      {
        name: '智慧办公',
        desc: '覆盖考勤、访客、会议和消费等办公场景，以智能时间管理和办公生态提升组织运营效率。',
        jobDirections: null,
        image: '/site/smart_office.jpg'
      },
      {
        name: '智慧商业',
        desc: '以AI与数字标牌技术服务零售、餐饮等行业，为商业空间提供数字化解决方案与运营支持。',
        jobDirections: null,
        image: '/site/smart_business.png'
      }
    ]
  },

  global: {
    title: '全球布局',
    desc: '从研发到制造，从区域中心到本地服务，熵基科技的能力网络覆盖全球主要市场。',
    mapImage: null,
    highlights: [
      { label: '研发中心', value: '5 个' },
      { label: '制造中心', value: '3 个' },
      { label: '分支机构', value: '100+ 个' },
      { label: '业务覆盖', value: '100+ 个国家和地区' }
    ],
    btnText: '探索招聘机会',
    btnTarget: 'SOCIAL'
  },

  /**
   * 主视觉之后、企业介绍之前：职位方向。
   * 卡片数据来自启用中的职位类别字典，这里只维护区块文案，以及已知类别的英文名/一句话（自定义类别走兜底）。
   */
  jobCategories: {
    title: '职位方向',
    subtitle: '按专业方向进入对应类别的在招职位，社招与校招都会出现在同一列表中。',
    allBtnText: '浏览全部在招职位',
    meta: {
      技术研发: { en: 'Engineering & Research', hint: '算法、嵌入式、云与端侧' },
      产品设计: { en: 'Product & Design', hint: '产品定义、交互与视觉' },
      市场销售: { en: 'Sales & Marketing', hint: '品牌、渠道与客户成功' },
      职能支持: { en: 'Corporate Support', hint: '人力、财务、法务与行政' },
      生产制造: { en: 'Manufacturing', hint: '工艺、质量与智能制造' },
      供应链: { en: 'Supply Chain', hint: '计划、采购与交付' }
    },
    fallback: { en: 'Open Roles', hint: '查看该方向正在招聘的职位' }
  },

  culture: {
    title: '企业文化与工作体验',
    values: [
      { name: '责任', desc: '对结果负责，也对同事和客户负责。' },
      { name: '正直', desc: '把事情说清楚，把承诺做到位。' },
      { name: '求实', desc: '用数据和事实说话，不做表面功夫。' },
      { name: '卓越', desc: '持续改进，把及格线之上的空间留给自己。' }
    ],
    cards: [
      { title: '办公环境', desc: '开放的园区与协作空间，让沟通与专注都有位置。', image: '/site/Office Environment.png' },
      { title: '学习成长', desc: '重视“传帮带”和学习型文化，在实践与协作中持续积累。', image: '/site/ZKstudy.png' },
      {
        title: '公益与可持续',
        desc: '坚持科技向善，积极履行企业社会责任，以创新和本地运营持续创造社会价值。',
        image: '/site/Community Public Welfare.png'
      }
    ]
  },

  cta: {
    title: '逐梦AI，与熵基一起成为 Change-Maker',
    subtitle: '无论你处在职业生涯的哪个阶段，这里都有一个可以持续创造的位置。',
    socialBtnText: '浏览社会招聘',
    campusBtnText: '浏览校园招聘'
  },

  footer: {
    address: '广东省东莞市塘厦镇平山工业大路32号',
    contactEmail: null,
    contactPhone: '0769-82109991',
    privacyUrl: 'https://www.zkteco.com/cn/privacy_policy',
    termsUrl: 'https://www.zkteco.com/cn/terms_of_use',
    qrcodeImage: '/site/wechatZK.jpg',
    icp: '本系统为演示环境，请勿提交真实个人信息'
  }
}
