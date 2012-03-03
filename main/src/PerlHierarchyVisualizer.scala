object PerlHierarchyVisualizer {
  def main(args:Array[String]) {
    val root = if (args.length > 0) args(0) else "."
    
    val perlElementsMap = FileMapper.filePathsMap((x:String)=>x, root)
              .filter(_.endsWith(".pm"))
              .foldLeft(Map.empty[String, PerlElement])(
                (map:Map[String, PerlElement], path:String) => {
                  val elm = PerlHierarchyInspector.extract(path)
                  map + (elm.packageName.getOrElse("UNPACKAGED") -> elm)
                }
              )
    println(Visualizer(perlElementsMap).create())
  }
}

trait Visualizer {
  def create():String
}
object Visualizer {
  def apply(elms:Map[String, PerlElement]): Visualizer = {
    new CytoscapeVisualizer(elms)
  }
}
class CytoscapeVisualizer(elms:Map[String, PerlElement]) extends Visualizer {
  def create():String = {
    elms.filter {
      case(key, value) => {
        !value.packageName.isEmpty
      }
    } map {
      case(key, value) => {
        if (value.basePackageName.isEmpty)
          value.packageName.get
        else
          "%s pd %s".format(value.packageName.get, value.basePackageName.get)
      }
    } reduceLeft {
      (x, y) => x + "\n" + y
    }
  }
}

case class PerlElement(packageName:Option[String], basePackageName:Option[String])

object PerlHierarchyInspector {
  import java.io.{FileInputStream, BufferedReader, InputStreamReader}
  import scala.util.control.Breaks.{ break, breakable }

  def extractPackageName(source:String): Option[String] = {
    val packageNameRegex = "^package\\s+(\\S+)\\s*;\\s*".r;
    source match {
      case packageNameRegex(r) => Option(r)
      case _ => None
    }
  }

  def extractBasePackageName(source:String): Option[String] = {
    def extractFromQuot(source:String) : Option[String] = {
      val basePackageNameRegex = "^use\\s+base\\s+['|\"]\\s*(\\S+)\\s*['|\"]\\s*;".r;
      source match {
        case basePackageNameRegex(r) => Option(r)
        case _ => None
      }
    }
    def extractFromQw(source:String) : Option[String] = {
      val basePackageNameRegex = "^use\\s+base\\s+qw.\\s*(\\S+)\\s*.\\s*;".r;
      source match {
        case basePackageNameRegex(r) => Option(r)
        case _ => extractFromQuot(source)
      }
    }
    extractFromQw(source)
  }

  def isCommentLine(source:String): Boolean = {
    val regex = "^\\s*#.*".r;
    !regex.findFirstIn(source).isEmpty
  }
  
  def extract(path:String) : PerlElement = {
    var packageName:Option[String] = None;
    var basePackageName:Option[String] = None;
    def check(text:String):Boolean = {
      // extract all elements or start POD
      !(packageName.isEmpty || basePackageName.isEmpty) || text.startsWith("=")
    }
    readFile((x:String) => {
      if (!isCommentLine(x)) {
        if (packageName.isEmpty) packageName = extractPackageName(x)
        if (basePackageName.isEmpty) basePackageName = extractBasePackageName(x)
      }
      check(x)
    }, path)
    PerlElement(packageName, basePackageName);
  }
  
  def readFile(f:(String)=>Boolean, path:String) {
    read(f, new FileInputStream(path))
  }

  def read(f:(String)=>Boolean, in:java.io.InputStream) {
    try {
      val reader = new BufferedReader(new InputStreamReader(in))
      var line:String = reader.readLine()
      breakable {
        while (line != null) {
          if (f( line )) break()
          line = reader.readLine()
        }
      }
    } finally {
      in.close();
    }
  }
}

object FileMapper {
  import java.io.File

  def filePathsMap[A](f: (String)=>A, root: File): List[A] =
    filesMap((x:File)=>f(x.getAbsolutePath), root)

  def filePathsMap[A](f: (String)=>A, root: String) : List[A] =
    filePathsMap(f, new File(root))

  def filesMap[A](f: (File)=>A, root: File): List[A] = {
    root match {
      case x if x.isFile => List(f(x))
      case x if x.isDirectory => x.listFiles.foldLeft(List.empty[A]){ _ ::: filesMap (f, _) }
    }
  }
}