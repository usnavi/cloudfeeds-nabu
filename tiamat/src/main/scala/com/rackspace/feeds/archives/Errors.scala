package com.rackspace.feeds.archives

object Errors {

  def IMPERSONATE( user : String, cause : String ) : String = {
    s"TIAMAT001: Unable to impersonate '${user}': ${cause}"
  }

  def ADMIN_TOKEN( cause : String ) : String = {
    s"TIAMAT002: Unable to get admin token: ${cause}"
  }

  def ADMIN_USER( tenant : String, cause : String ) : String = {
    s"TIAMAT003: Unable to get admin user for tenant '${tenant}': ${cause}"
  }

  def WRITE( path : String, cause : String ) : String = {
    s"TIAMAT004: Unable to write '${path}': ${cause}"
  }

  def CREATE_CONTAINER( uri : String, cause : String ) : String = {
    s"TIAMAT005: Unable to create container: '${uri}': ${cause}"
  }

  def CONTAINER_EXISTS( container : String, cause : String ) : String = {
    s"TIAMAT006: Unable to determine if container '${container}' exists: ${cause}"
  }

  val NO_CLOUD_FILES = "TIAMAT007: Unable to read/write to Cloud Files"

  def MISSING_SUCCESS_FILES(successFiles: String) : String = {
    s"TIAMAT008: Missing success files: [$successFiles]"
  }
}
