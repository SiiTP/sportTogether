module.exports = function (grunt) {

    grunt.initConfig({
        watch: {
            jsttojs: {
                files: ['templates/**/*.hbs'],
                tasks: ['jsttojs'],
                options: {
                    atBegin: true
                }
            },
            sass: {
                files: ['css/scss/**/*.scss'],
                tasks: ['sass'],
                options: {
                    atBegin: true
                }
            },
            server: {
                files: [
                    'js/**/*.js',
                    'css/**/*.css',
                    'index.html'
                ],
                options: {
                    interrupt: true,
                    livereload: true
                }
            }
        },
        shell: {
            options: {
                stdout: true,
                stderr: true
            },
            server: {
                command: 'sudo java -jar permanentTransactions-0.5.1.jar'
            }
        },
        jsttojs: {
            root: 'templates',
            output: 'js/tmpl/hbs_templates.js',
            ext: 'hbs',
            removebreak: true,
            amd: true,
            requirements: ['handlebars']
        },
        sass: {
            dist: {
                files: [{
                    expand: true,
                    cwd: 'css/scss',
                    src: ['*.scss'],
                    dest: 'css',
                    ext: '.css'
                }]
            }
        },

        requirejs: {
            build: {
                options: {
                    almond: true,
                    baseUrl: "js",
                    mainConfigFile: "js/main.js",
                    name: "main",
                    // wrap: true,
                    // include: ['main'],
                    // insertRequire: ['main'],
                    optimize: "none",
                    out: "js/build/main.js"
                }
            }
        },
        concat: {
            build: {
                separator: ';\n',
                src: [
                    'js/lib/almond.js',
                    'js/build/main.js'
                ],
                dest: 'js/build/build.js'
            }
        },
        uglify: {
            build: {
                files: {
                    'js/build/build.min.js':
                        ['js/build/build.js']
                }
            }
        },

        concurrent: {
            target: ['watch', 'shell'],
            options: {
                logConcurrentOutput: true
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-concurrent');
    grunt.loadNpmTasks('grunt-shell');
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-jsttojs');
    grunt.loadNpmTasks('grunt-contrib-requirejs');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-concat');

    grunt.registerTask('default', ['concurrent']);
    grunt.registerTask(
        'build',
        [
            'jsttojs', 'requirejs:build',
            'concat:build', 'sass:build'
        ]
    );
};
