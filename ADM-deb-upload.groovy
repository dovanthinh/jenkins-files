@Library('admin-groovy-libs@master') _

// Basic info and sanitization
def pkg = URLDecoder.decode(DEBURL)
String pkgName = ""
String pkgVer = ""
pkgInfo = pkgInfo.getPkgInfo(pkg)
pkgName = pkgInfo[0]
pkgVer = pkgInfo[1]

def workDir = "/sysadmin/packages"
def pkgFilename = pkg.split("/").last()

currentBuild.displayName = "#" + currentBuild.number + " - " + pkgName + "=" + pkgVer

node("master") {
    stage("Get DEB file") {
        sh(script: "python /sysadmin/jenkins-script/deb_download.py ${pkg} ${workDir}")
    }
    stage("Upload to repo") {
        if (PROD == "false") {
            sh(script: "python /sysadmin/jenkins-script/aptly_repo.py ${workDir}/${pkgFilename}")
        } else {
            sh(script: "python /sysadmin/jenkins-script/aptly_repo.py -p ${workDir}/${pkgFilename}")
        }
    }
}
