#!/bin/bash
# @Autor Ondrej Kopr
# Script for easy release backend and frontend modules
# Update all version backend and/or frontend modules
# Deploy backend and/or frontend modules
#
BACKTITLE="Czech IdM release script for product modules"
backendDIR="./backend/"
frontendDIR="./frontend/"
AGGREGATOR_MODULE_PATH="${backendDIR}aggregator"
SNAPSHOT="-SNAPSHOT"
FRONTEND_APP_PATH="${frontendDIR}czechidm-app"
CURRENT_BRANCH=`git branch | sed -n -e 's/^\* \(.*\)/\1/p'`
MASTER_BRANCH="master"
DEVELOP_BRANCH="develop"
ORIGINAL_PATH=`pwd`
CURRENT_VERSION=''
NEW_DEVELOPMENT_VERSION=''
RELEASE_VERSION=''

# TODO:
# 1. create debug mode
# 2. check white spaces in tag versions
# 3. check merge conflict before merge release to master
# 4. release/deploy only selected modules see selecbox
# 5. use the script for projspec modules

function resolveCurrentVersion() {
  CURRENT_VERSION=`cd $AGGREGATOR_MODULE_PATH && mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'`
  cd "$ORIGINAL_PATH"
}

function resolveNewDevelopmentVersion() {
  minorVersion=`echo $RELEASE_VERSION | sed -e 's/[0-9]*\.//g'`
  minorVersion=$(($minorVersion+1))
  NEW_DEVELOPMENT_VERSION=`echo $CURRENT_VERSION | sed -e "s/[0-9][0-9]*\([^0-9]*\)$/$minorVersion/"`
  NEW_DEVELOPMENT_VERSION+=$SNAPSHOT

  # resolve versions
  NEW_DEVELOPMENT_VERSION=$(whiptail --backtitle "${BACKTITLE}" --inputbox "New development version:" 8 78 "${NEW_DEVELOPMENT_VERSION}" --title "Development version" --nocancel 3>&1 1>&2 2>&3)
}

function resolveReleaseVersion() {
  # prepare version
  RELEASE_VERSION=`echo $CURRENT_VERSION | sed -e 's/[^0-9][^0-9]*$//'`

  # resolve versions
  RELEASE_VERSION=$(whiptail --backtitle "${BACKTITLE}" --inputbox "Release version:" 8 78 "${RELEASE_VERSION}" --title "Release version" --nocancel 3>&1 1>&2 2>&3)
}

function resolveNewVersion() {
  # prepare version
  RELEASE_VERSION=`echo $CURRENT_VERSION | sed -e 's/[^0-9][^0-9]*$//'`

  # resolve versions
  RELEASE_VERSION=$(whiptail --backtitle "${BACKTITLE}" --inputbox "Enter new version:" 8 78 "${RELEASE_VERSION}" --title "New version" --nocancel 3>&1 1>&2 2>&3)
}

function resolveVersion() {
  resolveReleaseVersion
  resolveNewDevelopmentVersion
  # user check version
  if (!(whiptail --backtitle "${BACKTITLE}" --title "Version detail" --yesno "Please check version if is correct\n\nCurrent version: ${currentVersion}\nRelease version: ${RELEASE_VERSION}\nNew development version: ${NEW_DEVELOPMENT_VERSION}" 11 78)) then
      echo "Exit, bad version."
      exit 1
  fi
}

function updateVersionReleaseBackend() {
  # update version (update version only for aggregator, the module has all dependecies + parent)
  cd "$AGGREGATOR_MODULE_PATH"
  mvn versions:update-parent -DparentVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false
  mvn versions:set -DnewVersion=${RELEASE_VERSION} -DprocessAllModules=true -DprocessParent=false -DgenerateBackupPoms=false
  mvn -N versions:update-child-modules -DgenerateBackupPoms=false
  cd "$ORIGINAL_PATH"
}

function updateVersionReleaseFrontend() {
  # update version on all frontend modules
  cd "$FRONTEND_APP_PATH"
  gulp versionSet --version ${RELEASE_VERSION,,}
  cd "$ORIGINAL_PATH"
}

function commitAndPushRelease() {
  commitMessage=$(whiptail --backtitle "${BACKTITLE}" --inputbox "Commit message with new version:" 8 78 "Release version ${RELEASE_VERSION} (FE+BE)" --title "Commit message" --nocancel 3>&1 1>&2 2>&3)
  tagName=$(whiptail --backtitle "${BACKTITLE}" --inputbox "Tag name for new version" 8 78 "${RELEASE_VERSION}" --title "Tag name" --nocancel 3>&1 1>&2 2>&3)
  tagMessage=$(whiptail --backtitle "${BACKTITLE}" --inputbox "Tag message for new version" 8 78 "Version ${RELEASE_VERSION}" --title "Tag message" --nocancel 3>&1 1>&2 2>&3)

  # add all changes to commit
  git add .

  # create commit and tag
  git commit -m "$commitMessage"
  git tag -a $tagName -m "$tagMessage"

  # push commit and tag
  git push
  git push --tags
}

function deployBackend() {
  # deploy BE
  cd "$AGGREGATOR_MODULE_PATH"
  mvn clean deploy -Prelease -DdocumentationOnly=true
  # TODO: debug mode, in production is required tests
  # if (whiptail --backtitle "${BACKTITLE}" --title "Skip tests?" --yesno --defaultno "Now will be process deploy to nexus, skip tests?" 8 78) then
  #   mvn clean deploy -Prelease -DdocumentationOnly=true -DskipTests=true
  # else
  #     mvn clean deploy -Prelease -DdocumentationOnly=true
  # fi
  cd "$ORIGINAL_PATH"
}

function deployFrontend() {
  cd "$FRONTEND_APP_PATH"
  gulp release --onlyPublish true
  cd "$ORIGINAL_PATH"
}

function mergeToMaster() {
  # merge to final branch
  branchForMerge=$(whiptail --backtitle "${BACKTITLE}" --inputbox "Branch for merge release" 8 78 "${MASTER_BRANCH}" --title "Master branch" --nocancel 3>&1 1>&2 2>&3)
  git checkout $branchForMerge
  git merge $CURRENT_BRANCH
  git push
  git checkout $CURRENT_BRANCH
}

function updateVersionDevelopBackend() {
  # update version (update version only for aggregator, the module has all dependecies + parent)
  cd "$AGGREGATOR_MODULE_PATH"
  mvn versions:update-parent -DparentVersion=${NEW_DEVELOPMENT_VERSION} -DgenerateBackupPoms=false
  mvn versions:set -DnewVersion=${NEW_DEVELOPMENT_VERSION} -DprocessAllModules=true -DprocessParent=false -DgenerateBackupPoms=false
  mvn -N versions:update-child-modules -DgenerateBackupPoms=false
  cd "$ORIGINAL_PATH"
}

function updateVersionDevelopFrontend() {
  # update version on all frontend modules
  cd "$FRONTEND_APP_PATH"
  gulp versionSet --version ${NEW_DEVELOPMENT_VERSION,,}
  cd "$ORIGINAL_PATH"
}

function commitAndPushDevelop() {
  commitMessage=$(whiptail --backtitle "${BACKTITLE}" --inputbox "Commit message with new develop version:" 8 78 "New develop version ${NEW_DEVELOPMENT_VERSION} (FE+BE)" --title "Commit message" --nocancel 3>&1 1>&2 2>&3)

  # add all changes to commit
  git add .

  # create commit and tag
  git commit -m "$commitMessage"
  git push
}

function completeRelease() {
  # check git pull and untracked files and git push
  # if [ -n "$(git status --porcelain)" ]; then
  #   whiptail --textbox /dev/stdin 12 80 <<<"You have uncommited changes.\n\nPlease commit all changes. To current branch: $CURRENT_BRANCH"
  #   exit
  # fi

  # check push and pull on git
  git pull
  git push

  resolveCurrentVersion
  resolveVersion
  updateVersionReleaseBackend
  updateVersionReleaseFrontend
  commitAndPushRelease
  # deploy nexus
  deployBackend
  deployFrontend
  mergeToMaster
  updateVersionDevelopBackend
  updateVersionDevelopFrontend
  commitAndPushDevelop
}

# MAIN CONTEXT

# MENU
CHOICE=$(whiptail --backtitle "${BACKTITLE}" --title "Release menu" --menu "Choose an option" 25 78 16 \
  "1" "Complete release for all modules" \
  "2" "Update version (all)" \
  "3" "Update version (only backend)" \
  "4" "Update version (only frontend)" \
  "5" "Deploy to nexus (all)" \
  "6" "Deploy to nexus (only backend)" \
  "7" "Deploy to nexus (only  frontend)" \
  "8" "exit" 3>&2 2>&1 1>&3 )

  case "$CHOICE" in
  "1")
      completeRelease
      echo "Release was done"
      ;;
  "2")
      resolveCurrentVersion
      resolveNewVersion
      updateVersionReleaseBackend
      updateVersionReleaseFrontend
      echo "Backend + Frontend version was updated"
      ;;
  "3")
      resolveCurrentVersion
      resolveNewVersion
      updateVersionReleaseBackend
      echo "Backend version was updated"
      ;;
  "4")
      resolveCurrentVersion
      resolveNewVersion
      updateVersionReleaseFrontend
      echo "Frontend version was updated"
      ;;
  "5")
      deployBackend
      deployFrontend
      echo "Backend + Frontend version was deployed to nexus"
      ;;
  "6")
      deployBackend
      echo "Backend version was deployed to nexus"
      ;;
  "7")
      deployFrontend
      echo "Frontend version was deployed to nexus"
      ;;
  "8")
      exit
      ;;
  *)
      echo "Bad value"
      exit 1
      ;;
  esac


# check modules to process
# backendModules=()
# for directory in $(find $backendDIR* -maxdepth 0 -type d )
# do
#     moduleName=`basename ${directory}`
#     backendModules+=($directory "Release module $moduleName " ON)
# done
# modules=$(whiptail --backtitle "${BACKTITLE}" --title "Check list modules for release" --checklist "Choose modules for release" 20 108 15 "${backendModules[@]}" 3>&1 1>&2 2>&3)

exit 0
