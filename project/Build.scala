/**
 * orm-only build definition for sbt 0.11 & scala 2.9.1
 */
import sbt._
import Keys._

object MyBuild extends Build {
  lazy val ormOnlyProject = Project("orm-only", file(".")) settings(

      /*
       * lib_managed directory will be automatically created.
       * these jars goes to therer.
       */
      retrieveManaged := true,
      libraryDependencies += "asm" % "asm" % "3.3.1",
      libraryDependencies += "c3p0" % "c3p0" % "0.9.1.2",
      libraryDependencies += "cglib" % "cglib" % "2.2.2",
      libraryDependencies += "net.sf.ehcache" % "ehcache-core" % "2.3.0",
      libraryDependencies += "commons-io" % "commons-io" % "2.1",
      libraryDependencies += "javax.transaction" % "jta" % "1.1",

      /**
       * orm-only needs these three sources.
       */
      unmanagedSourceDirectories in Compile <+= 
        baseDirectory{ _ / "circumflex-cache" / "src" / "main" / "scala"},  
      unmanagedSourceDirectories in Compile <+= 
        baseDirectory{ _ / "circumflex-core" / "src" / "main" / "scala"},      
      unmanagedSourceDirectories in Compile <+= 
        baseDirectory{ _ / "circumflex-orm" / "src" / "main" / "scala"}      
  ) 
}
