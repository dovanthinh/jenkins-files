@Library('admin-groovy-libs@master') _

def build_pkg
def fpmOpts = ""
if (PYTHONVER == "3") {
    fpmOpts = "--python-bin /usr/bin/python3 --python-package-name-prefix python3"
}

node("master") {
    cleanWs()
}

currentBuild.displayName = "#" + currentBuild.number + " - python" + PYTHONVER + "-" + MODULE

node("master") {
    stage("Build Deb") {
        def build = sh(script: "fpm -s python -t deb ${fpmOpts} ${MODULE}", returnStdout: true)
        build.tokenize("\n").each { line ->
            if (line.contains('Created package')) {
                built_pkg = line.split("\"")[-2]
            }
        }
    }
    stage("Upload Repo") {
        if (REPO != "") {
            sh(script: "python /sysadmin/jenkins/script/aptly_repo.py ${build_pkg} -r ${REPO}")
        }
    }    
}

