

case class Repo( fileField:String, title:String, id:String ) {
    def asJson = 
    s"""|"$fileField": {
        |     "title": "$title",
        |     "id": "$id"
        | }""".stripMargin
}

object Repo {
    def fromRecord(arr:Seq[String]):Repo = {
        val fileField = arr(0).replace("\"","").split("\\.")(0)
        var title = arr(1).replace("\"","")
        var id = ""
        if ( title.contains("(") ) {
            val cs = title.split("\\(")
            title = cs(0)
            id = cs(1).split("\\)")(0)
        } else {
            id = title.filter(_.isUpper).mkString
        }
        if ( title.nonEmpty ) {
            Repo( fileField, title.trim, id.trim )
        } else {
            Repo( fileField, fileField, arr(2) )
        }
    }
} 

class CsvRecordReader( in:String ) {
    val fields = collection.mutable.Buffer[String]()

    var curField = collection.mutable.Buffer[Char]();
    var inQuoted = false;
    var skipNext = false;
    for ( c <- in ) {
        if ( skipNext ) {
            skipNext = false
        } else {
            c match {
                case '\"' => if ( inQuoted ) {
                    inQuoted = false
                    fields+=curField.mkString
                    skipNext=true
                    curField.clear()
                } else {
                    inQuoted = true
                }
                case ',' =>  if ( inQuoted ) {
                    curField += c
                } else {
                    fields+=curField.mkString
                    curField.clear()
                }
                case _ => curField += c
            }

        }
    }
    fields+=curField.mkString

    def getFields:Seq[String] = fields.toSeq.map(_.trim)
}

object Main extends App {

    if ( args.length == 0 ) {
        println( "usage: csvToRepoNames <modelName> <csv file>" )
        System.exit(-1)
    }

    val repos = io.Source.fromFile( new java.io.File(args(1)) ).getLines()
        .map( line => new CsvRecordReader(line).getFields )
        .filter( _(3)==args(0) )
        .map( Repo.fromRecord(_) )

    println(
        repos.map( _.asJson ).mkString("{", "\n,", "}")
    )
}