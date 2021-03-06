#!/usr/bin/env python
import argparse
import os
import shutil
import signal
import subprocess
import sys
import time
import zipfile

DEPLOY_DIR = "/home/server-snapshots"
CURRENT_DEPLOY_DIR = os.path.join(DEPLOY_DIR, "whereuat-server")
BACKUP_DIR = os.path.join(DEPLOY_DIR, "_whereuat-server")
TMP_DIR = os.path.join(DEPLOY_DIR, "tmp")


# Make a new directory in the deploying directory and return the path of the new
# directory.
def makeNewDir():
  try:
    os.mkdir(DEPLOY_DIR)
  except OSError as e:
    # If DEPLOY_DIR already exists
    pass
  new_dir = "{:0.0f}".format(time.time())
  new_path = os.path.join(DEPLOY_DIR, new_dir)
  os.mkdir(new_path)
  return new_path


# Delete the old backup.
def removeBackup():
  try:
    shutil.rmtree(BACKUP_DIR)
  except OSError as e:
    return


# Rename the current deploy to be the backup.
def makeCurrentDeployBackup():
  try:
    shutil.move(CURRENT_DEPLOY_DIR, BACKUP_DIR)
  except IOError as e:
    if e.errno == 2:
      print "Current deploy directory {} doesn't exist.".format(
        CURRENT_DEPLOY_DIR)


# Unzip _zf_ and put it in _dest_.
def unzip(zf, dest):
  # Create the temp directory
  try:
    os.mkdir(TMP_DIR)
  except OSError as e:
    pass

  # Extract the zip file
  with zipfile.ZipFile(zf, 'r') as zip_ref:
    zip_ref.extractall(TMP_DIR)

  # Rename the extracted directory to be the current deploy directory
  zip_name = zf.replace(".zip", "").split('/')[-1]
  zip_dir = os.path.join(TMP_DIR, zip_name) 
  shutil.move(zip_dir, CURRENT_DEPLOY_DIR)

  # Remove temp directory
  try:
    shutil.rmtree(TMP_DIR)
  except OSError as e:
    # If TMP_DIR already does not exist
    pass

  # Change the permissions on the unzipped files.
  for root, dirs, files in os.walk(CURRENT_DEPLOY_DIR):
    for d in dirs:
      os.chmod(os.path.join(root, d), 0777)
    for f in files:
      os.chmod(os.path.join(root, f), 0777)


# Start a new process running the server.
def startServer():
  exec_path = os.path.join(CURRENT_DEPLOY_DIR, "bin", "whereuat-server")
  try:
    with open(os.path.join("/dev", "null")) as dev_null:
      try:
        subprocess.Popen([exec_path], 
                         stdout=dev_null,
                         stderr=dev_null,
                         cwd=CURRENT_DEPLOY_DIR)
      except OSError as e:
        # Error in Popen
        print "ERROR: Server process could not be started"
        exit(1)
  except IOError as e:
    # Error in opening /dev/null
    print "ERROR: Unable to open /dev/null"
    exit(1)


# If the server is currently running, kill it.
def stopServer():
  pid_f = os.path.join(CURRENT_DEPLOY_DIR, "RUNNING_PID")
  try:
    with open(pid_f, 'r') as pid_ref:
      pid = int(pid_ref.readline().strip())
    os.kill(pid, signal.SIGTERM)
  except (IOError, OSError) as e:
    # If pid file does not exist, return
    return


def main(zf):
  new_path = makeNewDir()
  shutil.copy(zf, new_path)
  removeBackup()
  stopServer()
  makeCurrentDeployBackup()
  unzip(zf, CURRENT_DEPLOY_DIR)
  startServer()


if __name__ == "__main__":
  parser = argparse.ArgumentParser()
  parser.add_argument("zip_file")
  args = parser.parse_args()
  main(args.zip_file)
