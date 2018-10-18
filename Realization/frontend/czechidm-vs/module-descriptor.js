module.exports = {
  'id': 'vs',
  'npmName': 'czechidm-vs',
  'backendId': 'vs',
  'name': 'Virtual system for CzechIdM',
  'description': 'Virtual system module for CzechIdM. Provides system connector for connecting CzechIdM systems. Virtual systems are used in situations, when connector operations (create/update/delete accounts) have to be done (implemented) manually. Basically, the virtual system creates a task for the implementer (admin of the real system) for every active connector operation.',
  // 'mainStyleFile': 'src/css/main.less',
  'mainRouteFile': 'routes.js',
  'mainComponentDescriptorFile': 'component-descriptor.js',
  'mainLocalePath': 'src/locales/',
  'navigation': {
    'items': [
      {
        'id': 'vs-main-menu',
        'labelKey': 'vs:content.virtuals.label',
        'titleKey': 'vs:content.virtuals.title',
        'icon': 'link',
        'iconColor': '#008AFF',
        'path': '/vs/systems',
        'order': 1101,
        'access': [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SYSTEM_READ', 'VSREQUEST_READ'] } ],
        'items': [
          {
            'id': 'vs-systems',
            'type': 'DYNAMIC',
            'section': 'main',
            'labelKey': 'vs:content.vs-systems.label',
            'titleKey': 'vs:content.vs-systems.title',
            'order': 10,
            'icon': 'link',
            'iconColor': '#008AFF',
            'path': '/vs/systems',
            'access': [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SYSTEM_READ'] } ]},
          {
            'id': 'vs-requests',
            'type': 'DYNAMIC',
            'section': 'main',
            'labelKey': 'vs:content.vs-requests.label',
            'titleKey': 'vs:content.vs-requests.title',
            'order': 20,
            'icon': 'inbox',
            'iconColor': '#008AFF',
            'path': '/vs/requests',
            'access': [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['VSREQUEST_READ'] } ]
          }
        ]
      }
    ]
  }
};
