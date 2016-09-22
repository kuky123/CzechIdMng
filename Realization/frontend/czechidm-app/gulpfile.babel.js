import fs from 'fs';
import gulp from 'gulp';
import path from 'path';
import browserify from 'browserify';
import watchify from 'watchify';
import source from 'vinyl-source-stream';
import buffer from 'vinyl-buffer';
import eslint from 'gulp-eslint';
import bootlint from 'gulp-bootlint';
import babelify from 'babelify';
import uglify from 'gulp-uglify';
import rimraf from 'rimraf';
import notify from 'gulp-notify';
import browserSync, { reload } from 'browser-sync';
import sourcemaps from 'gulp-sourcemaps';
import htmlReplace from 'gulp-html-replace';
import image from 'gulp-image';
import runSequence from 'run-sequence';
import less from 'gulp-less';
import minifyCSS from 'gulp-minify-css';
import autoprefixer from 'gulp-autoprefixer';
import mocha from 'gulp-mocha';
import stringify from 'stringify';
import yargs from 'yargs';
import util from 'gulp-util';
import pathmodify from 'pathmodify';
import flatmap from 'gulp-flatmap';
import replace from 'gulp-replace';
import concat from 'gulp-concat';
import vfs from 'vinyl-fs';
import inject from 'gulp-inject';

require('babel/register');

const paths = {
  bundle: 'app.js',
  srcJs: ['node_modules/bootstrap-less/js/bootstrap.min.js', 'node_modules/jquery/dist/jquery.min.js', 'node_modules/metismenu/dist/metisMenu.min.js'],
  srcFont: ['node_modules/bootstrap-less/fonts/*', 'src/fonts/*', 'node_modules/font-awesome/fonts/*'],
  srcJsx: 'src/Index.js',
  srcLess: ['src/css/main.less'],
  srcIncludedLess: [],
  srcCssSeparate: ['src/css/google.fonts.css'], // this css will be add as separate files (not conacat to main.less)
  srcImg: 'images/**', // client images
  srcThemes: ['images/**'], // only images for now
  srcLocale: [], // will filled during compile (with locales from submodules)
  srcModuleDescriptor: 'node_modules/**/module-descriptor.js',
  srcModuleAssembler: 'src/modules/moduleAssembler.js', // default modules assembler
  srcRouteAssembler: 'src/modules/routeAssembler.js', // default routes assembler
  srcComponentAssembler: 'src/modules/componentAssembler.js', // default component assembler
  testSrc: ['test/**/*.js', './czechidm-modules/*/test/**/**/*.js'],
  dist: 'dist',
  distJs: 'dist/js',
  distImg: 'dist/images',
  distCss: 'dist/css',
  distFont: 'dist/fonts',
  distLocale: 'dist/locales',
  distThemes: 'dist/themes',
  distModule: 'dist/modules',
  src: 'src/**/*.js'
};

const pathmodifyOptions = {
  mods: [
    pathmodify.mod.dir('app', path.join(__dirname, 'src'))
  ]
};
const compileMark = '// <compile mark>';
let modulesAssemblerContent = '';
let routesAssemblerContent = '';
let componentsAssemblerContent = '';

/**
 * Returns configuration for requestet environment
 * @param  {string} env environment
 * @return {object}     config json
 */
function getConfigByEnvironment(env = 'development', profile = 'default') {
  return require('./config/' + profile + '/' + env + '.json');
}

/**
 * Select environment stage and profile by input arguments.
 */
function selectStageAndProfile() {
  const argv = yargs.alias('p', 'profile').alias('s', 'stage').usage('Usage: $0 --profile [name of profile] --stage [development/test/production]')
  .choices('stage', ['development', 'test', 'production']).help('help').alias('h', 'help').argv;
  let profile = argv.profile;
  if (!profile) {
    profile = 'default';
    util.log('No profile argument present. Profile "' + profile + '" will be used for build!');
  } else {
    util.log('Profile "' + profile + '" will be used for build.');
  }
  let stage = argv.stage;
  if (!stage) {
    stage = 'development';
    util.log('No stage argument present. Stage "' + stage + '" will be used for build!');
  } else {
    util.log('Stage "' + stage + '" will be used for build.');
  }
  process.env.NODE_ENV = stage;
  process.env.NODE_PROFILE = profile;
}

gulp.task('makeModules', () => {
  return vfs.src('./czechidm-modules/**', {followSymlinks: false})
  .pipe(vfs.symlink('./node_modules'));
});

/**
 * Load module-descriptors.
 * Move them to dist.
 * Generate content for module assembler (add to global variable).
 */
gulp.task('loadModules', () => {
  return gulp.src(paths.srcModuleDescriptor)
  .pipe(flatmap(function loadModule(stream, file) {
    const descriptor = require(file.path);
    if (descriptor.npmName) {
      util.log('Loaded module-descriptor with ID:', descriptor.id);
      const pathRelative = descriptor.npmName + '/module-descriptor.js';
      // Add row to module assembler
      modulesAssemblerContent = modulesAssemblerContent + ' moduleDescriptors = moduleDescriptors.set("'
      + descriptor.id + '", require("' + pathRelative + '"));' + '\n';
    }
    return stream;
  }));
});

/**
 * Create final module assembler
 * Add paths on module descriptors to modules assembler file.
 * Move modules assembler to dist.
 */
gulp.task('createModuleAssembler', () => {
  return gulp.src(paths.srcModuleAssembler)
  .pipe(replace(compileMark, modulesAssemblerContent))
  .pipe(gulp.dest(paths.distModule));
});

/**
 * Load main styles form modules and add them to css paths array.
 */
gulp.task('loadModuleStyles', () => {
  return gulp.src(paths.srcModuleDescriptor)
  .pipe(flatmap(function loadModule(stream, file) {
    const descriptor = require(file.path);
    util.log('Loading style for module with ID:', descriptor.id);
    if (descriptor.mainStyleFile) {
      const fullStylePath = file.path.substring(0, file.path.lastIndexOf('module-descriptor.js')) + descriptor.mainStyleFile;
      util.log('Main module style file path:', fullStylePath);
      paths.srcIncludedLess.push(fullStylePath);
    }
    return stream;
  }));
});

gulp.task('loadModuleRoutes', () => {
  return gulp.src(paths.srcModuleDescriptor)
  .pipe(flatmap(function loadModule(stream, file) {
    const descriptor = require(file.path);
    if (descriptor.mainRouteFile && descriptor.npmName) {
      util.log('Loading routes for module with ID:', descriptor.id);
      const fullRoutePath = file.path.substring(0, file.path.lastIndexOf('module-descriptor.js')) + descriptor.mainRouteFile;
      util.log('Main module route file path:', fullRoutePath);
      const relativeRoutePath = descriptor.npmName + '/' + descriptor.mainRouteFile;
      // Add row to route assembler
      routesAssemblerContent = routesAssemblerContent + 'require("' + relativeRoutePath + '"),' + '\n';
    }
    return stream;
  }));
});

/**
 * Create final routes assembler
 * Add paths on module routes to modules assembler file.
 * Move routes assembler to dist.
 */
gulp.task('createRouteAssembler', () => {
  return gulp.src(paths.srcRouteAssembler)
  .pipe(replace(compileMark, routesAssemblerContent))
  .pipe(gulp.dest(paths.distModule));
});


gulp.task('loadModuleComponents', () => {
  return gulp.src(paths.srcModuleDescriptor)
  .pipe(flatmap(function loadModule(stream, file) {
    const descriptor = require(file.path);
    if (descriptor.mainComponentDescriptorFile && descriptor.npmName) {
      util.log('Loading components for module with ID:', descriptor.id);
      const fullComponentPath = file.path.substring(0, file.path.lastIndexOf('module-descriptor.js')) + descriptor.mainComponentDescriptorFile;
      util.log('Main module route file path:', fullComponentPath);
      const relativeComponentPath = descriptor.npmName + '/' + descriptor.mainComponentDescriptorFile;
      // Add row to component assembler
      componentsAssemblerContent = componentsAssemblerContent + ' componentDescriptors = componentDescriptors.set("'
      + descriptor.id + '", require("' + relativeComponentPath + '"));' + '\n';
    }
    return stream;
  }));
});

/**
 * Create final component assembler
 * Add requires on components descriptors (for each fined module) to components assembler file.
 * Move components assembler to dist.
 */
gulp.task('createComponentAssembler', () => {
  return gulp.src(paths.srcComponentAssembler)
  .pipe(replace(compileMark, componentsAssemblerContent))
  .pipe(gulp.dest(paths.distModule));
});


gulp.task('clean', cb => {
  rimraf('dist', cb);
});

gulp.task('browserSync', () => {
  browserSync({
    server: {
      baseDir: './'
    }
  });
});

gulp.task('watchify', () => {
  const bundler = watchify(
    browserify(paths.srcJsx, watchify.args)
      .plugin(pathmodify, pathmodifyOptions)
      .transform(stringify)
  );

  function rebundle() {
    return bundler
      .bundle()
      .on('error', notify.onError())
      .pipe(source(paths.bundle))
      .pipe(gulp.dest(paths.distJs))
      .pipe(reload({stream: true}));
  }

  bundler.transform(babelify)
  .on('update', rebundle);
  return rebundle();
});

gulp.task('browserify', () => {
  browserify(paths.srcJsx)
  .plugin(pathmodify, pathmodifyOptions)
  .transform(stringify)
  .transform(babelify)
  .bundle()
  .pipe(source(paths.bundle))
  .pipe(buffer())
  .pipe(sourcemaps.init())
  .pipe(
    uglify({
      compress: {
        global_defs: {
          DEBUG: false
        }
      }
    })
  )
  .pipe(sourcemaps.write('.'))
  .pipe(gulp.dest(paths.distJs));
});

gulp.task('styles', () => {
  util.log('Main application less:', paths.srcLess);
  util.log('Less for include to main:', paths.srcIncludedLess);
  const config = getConfigByEnvironment(process.env.NODE_ENV, process.env.NODE_PROFILE);
  //
  return gulp.src(paths.srcLess)
    .pipe(sourcemaps.init())
    /**
     * Dynamically injects @import statements into the main app.less file, allowing
     * .less files to be placed around the app structure with the component
     * or page they apply to.
     */
    .pipe(inject(gulp.src(paths.srcIncludedLess, {read: false}), {
      starttag: '/* inject:imports */',
      endtag: '/* endinject */',
      transform: function transform(filepath) {
        return '@import "' + __dirname + filepath + '";';
      }
    }))
    .pipe(less({
      compress: true,
      globalVars: {
        ENV: config.env,
        version: 10,
        theme: '\"' + config.theme + '\"' // wrap to quotes - less engine needs it to skip formating slash characters
      }
    }))
    .pipe(autoprefixer('last 10 versions', 'ie 9'))
    .pipe(minifyCSS({keepBreaks: false}))
    .pipe(concat('main.css'))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(paths.distCss))
    .pipe(reload({stream: true}))
    .pipe(gulp.src(paths.srcCssSeparate))
    .pipe(gulp.dest(paths.distCss));
});

gulp.task('htmlReplace', () => {
  const config = getConfigByEnvironment(process.env.NODE_ENV, process.env.NODE_PROFILE);
  //
  return gulp.src(['index.html'])
  .pipe(htmlReplace(
    {
      icon: {
        src: config.theme + '/images/favicon.ico',
        tpl: '<link rel="shortcut icon" href="%s" type="image/x-icon" />'
      },
      favicon: {
        src: config.theme + '/images/favicon.gif',
        tpl: '<link rel="icon" href="%s" type="image/gif" />'
      },
      css: ['css/main.css', 'css/google.fonts.css'],
      js: ['js/jquery.min.js', 'js/bootstrap.min.js', 'js/metisMenu.min.js', 'configUrl.js', 'js/app.js']
    })
  )
  .pipe(gulp.dest(paths.dist));
});

gulp.task('images', () => {
  return gulp.src(paths.srcImg)
  .pipe(image())
  .pipe(gulp.dest(paths.distImg));
});

gulp.task('themes', () => {
  const config = getConfigByEnvironment(process.env.NODE_ENV, process.env.NODE_PROFILE);
  if (config.theme) {
    const themeFullPath = path.join(__dirname, '/node_modules/', config.theme);
    util.log('Theme will load form path:', themeFullPath);
    return gulp.src(path.join(themeFullPath, '/images/**'))
    .pipe(gulp.dest(paths.distImg))
    // Find theme styles and add them to srcLess array
    .pipe(gulp.src(path.join(themeFullPath, '/css/*'))
    .pipe(flatmap(function loadModule(stream, file) {
      util.log('Add theme style from:', file.path);
      paths.srcIncludedLess.push(file.path);
      return stream;
    })));
  }
});

gulp.task('js', () => {
  return gulp.src(paths.srcJs)
    .pipe(gulp.dest(paths.distJs));
});

gulp.task('fonts', () => {
  return gulp.src(paths.srcFont)
    .pipe(gulp.dest(paths.distFont));
});

/**
 * Load locales form modules and copy them to dist.
 */
gulp.task('loadModuleLocales', () => {
  return gulp.src(paths.srcModuleDescriptor)
  .pipe(flatmap(function loadModule(stream, file) {
    const descriptor = require(file.path);
    if (descriptor.mainLocalePath) {
      util.log('Loading locale for module with ID:', descriptor.id);
      const fullLocalesPath = path.join(file.path.substring(0, file.path.lastIndexOf('module-descriptor.js')), descriptor.mainLocalePath, '*.json');
      util.log('Main module locale file path:', fullLocalesPath);
      paths.srcLocale.push(fullLocalesPath); // For watch purpose
      return gulp.src(fullLocalesPath)
      .pipe(gulp.dest(path.join(paths.distLocale, '/', descriptor.id, '/')));
    }
    return stream;
  })).pipe(reload({stream: true}));
});

gulp.task('lint', () => {
  return gulp.src(paths.srcJsx)
    .pipe(eslint())
    .pipe(eslint.format())
    .pipe(bootlint());
});

gulp.task('config', (cb) => {
  return fs.writeFile(path.join(__dirname, paths.dist, '/config.json'), JSON.stringify(getConfigByEnvironment(process.env.NODE_ENV, process.env.NODE_PROFILE)), cb);
});

gulp.task('urlConfig', (cb) => {
  const configuration = getConfigByEnvironment(process.env.NODE_ENV, process.env.NODE_PROFILE);
  return fs.writeFile(path.join(__dirname, paths.dist, '/configUrl.js'), 'serverUrl = \'' + configuration.serverUrl + '\';\n', cb);
});

gulp.task('test', () => {
  const argv = yargs.alias('w', 'watch').help('help').alias('h', 'help')
  .usage('Usage (for only one run test): gulp test --profile [name of profile] --stage [development/test/production]\nUsage (for permanent watch on src and test changes): gulp test --watch').argv;
  const watchArg = argv.watch;
  if (watchArg) {
    runSequence('clean', 'makeModules', 'loadModules', 'createModuleAssembler', 'loadModuleStyles', 'loadModuleRoutes', 'createRouteAssembler', 'loadModuleComponents', 'createComponentAssembler', 'themes');
    gulp.watch([paths.testSrc, paths.bundle], ['runTest']);
  } else {
    selectStageAndProfile();
    runSequence('clean', 'makeModules', 'loadModules', 'createModuleAssembler', 'loadModuleStyles', 'loadModuleRoutes', 'createRouteAssembler', 'loadModuleComponents', 'createComponentAssembler', 'themes', 'runTest');
  }
});

gulp.task('runTest', () => {
  // https://www.npmjs.com/package/gulp-mocha#require
  return gulp.src(paths.testSrc, { read: false })
    .pipe(mocha({
      reporter: 'nyan',
      recursive: true,
      compilers: ['js:babel/register'],
      require: ['./test/setup.js'],
      ignoreLeaks: false
    }));
});

gulp.task('watchTask', () => {
  gulp.watch(paths.srcLess, ['styles']);
  gulp.watch(paths.srcIncludedLess, ['styles']);
  gulp.watch(paths.srcJsx, ['lint']);
  gulp.watch(paths.srcLocale, ['loadModuleLocales']);
});

gulp.task('watch', cb => {
  selectStageAndProfile();
  runSequence('clean', 'makeModules', 'loadModules', 'createModuleAssembler', 'loadModuleStyles', 'loadModuleRoutes', 'createRouteAssembler', 'loadModuleComponents', 'createComponentAssembler', 'themes', 'runTest', 'config', 'urlConfig', 'styles', 'lint', 'images', 'js', 'fonts', 'loadModuleLocales', 'browserSync', 'watchTask', 'watchify', cb);
});

gulp.task('build', cb => {
  selectStageAndProfile();
  runSequence('clean', 'makeModules', 'loadModules', 'createModuleAssembler', 'loadModuleStyles', 'loadModuleRoutes', 'createRouteAssembler', 'loadModuleComponents', 'createComponentAssembler', 'themes', 'runTest', 'config', 'urlConfig', 'styles', 'htmlReplace', 'images', 'js', 'fonts', 'loadModuleLocales', 'browserify', cb);
});

gulp.task('default', ['watch']);
