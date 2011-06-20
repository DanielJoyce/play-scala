package play.db

import play.utils.Scala.MayErr
import play.utils.Scala.MayErr._

package anorm {

    trait MParser2[A1,A2,R] extends ParserWithId[R] {
        val p1:ColumnTo[A1]
        val p2:ColumnTo[A2]
        def apply(a1:A1,a2:A2):R
        val containerName:String
        val columnNames:(String,String)

        lazy val (name1,name2) = columnNames

        import SqlParser._
        override def apply(input:Input):SqlParser.ParseResult[R] = 
            (get[A1](name1)(p1) ~< get[A2](name2)(p2) ^^ {case a1 ~ a2 => apply(a1,a2)} )(input)

        val uniqueId : (Row=> MayErr[SqlRequestError,Any]) = null
    }

    abstract class MagicParser2[A1,A2,R]( 
        conventions: PartialFunction[AnalyserInfo,String] = asIs,
        table: Option[String]=None,
        columns:Option[(String,String)] = None)
        (implicit c1:ColumnTo[A1], c2:ColumnTo[A2], r:Manifest[R]) extends MParser2[A1,A2,R] {

        lazy val p1 = c1
        lazy val p2 = c2
         
        //needs clean

        lazy val typeName = r.erasure.getSimpleName
        lazy val containerName = table.orElse(conventions.lift(TableC(typeName))).getOrElse(typeName)

        import java.lang.reflect.Method

        def getParametersNames(m:Method):Seq[String] = {
            import scala.collection.JavaConversions._
            play.classloading.enhancers.LocalvariablesNamesEnhancer.lookupParameterNames(m)
        }

        lazy val columnNames = columns.getOrElse {
            implicitly[Manifest[this.type]]
                       .erasure
                       .getDeclaredMethods()
                       .filter(_.getName()=="apply")
                       .find(_.getParameterTypes().length == 2)
                       .map(getParametersNames)
                       .map( _.map(c =>  conventions.lift(ColumnC(typeName,c)).getOrElse(typeName + "." + c) ))
                       .collect{case Seq(a1,a2) => (a1,a2)}
                       .get

        }
    }

    trait M2[A1,A2,R] {
        self: MParser2[A1,A2,R] =>

        val pt1:(ColumnTo[A1],ToStatement[A1])
        val pt2:(ColumnTo[A2],ToStatement[A2])

        

        def unapply(r:R):Option[(A1,A2)]

        // override val conventions: PartialFunction[AnalyserInfo,String] = asIs 

        def update(v:R)(implicit hasId: (A1 <:< Pk[_]) |:| (A2 <:< Pk[_])) = {

            val all = ((v,hasId) match {
            case (self(a1,a2), (e1 |:| e2)) => 
                List ( (e1,name1, toParameterValue(a1)(pt1._2)),
                       (e2,name2, toParameterValue(a2)(pt2._2)))
            })

            val (ids,toSet) = all.partition(_._1.isDefined)
            if(ids == all) throw new Exception("everything is a Pk, nothing left to set!")

            val toUpdate = toSet.map(_._2).map(n => n+" = "+"{"+n+"}").mkString(", ")

            import Sql._

            sql("update "+containerName +" set "+toUpdate+
                " where "+ ids.map(_._2).map( n => n+" = "+"{"+n+"}").mkString(" and ") )
                .on(all.map(v =>  (v._2,v._3)): _* )
                .executeUpdate()
        }

        def create(v:R)(implicit hasId: (A1 <:< Pk[_]) |:| (A2 <:< Pk[_])): R = {
            val all = (v,hasId) match {
                case (self(a1,a2), (e1 |:| e2)) => 
                List( (e1,name1, toParameterValue(a1)(pt1._2)),
                      (e2,name2, toParameterValue(a2)(pt2._2) ) ) 
            }

            val (notSetIds,toSet) = all.partition(e => e._1.isDefined && e._3.aValue==NotAssigned)

            if(notSetIds.length > 1) throw new Exception("multi ids not supported")
            val toInsert = toSet.map(_._2)

            import Sql._
            import scala.util.control.Exception._
            import SqlParser._

            val idParser:SqlParser.Parser[_] = {
                SqlParser.RowParser(row =>
                    row.asList.headOption.flatMap(a =>
                        (if (a.isInstanceOf[Option[_]]) a else Option(a)).asInstanceOf[Option[_]]
                    ).toRight(NoColumnsInReturnedResult)
                )
            }

            val (statement, ok) = sql("insert into "+containerName+" ( "+toInsert.mkString(", ")+" ) values ( "+toInsert.map("{"+_+"}").mkString(", ")+")")
                .on(all.map(v =>  (v._2,v._3)): _* )
                .execute1(getGeneratedKeys=true)

            val rs = statement.getGeneratedKeys();
            val id = idParser(StreamReader(Sql.resultSetToStream(rs))).get
            val List(a1,a2) = all.map(_._3.aValue).map({case NotAssigned => Id(id); case other => other})
            apply(a1.asInstanceOf[A1],a2.asInstanceOf[A2])
        }
    }


    abstract class Magic2[A1,A2,R](table: Option[String]=None,names:Option[(String,String)] =None)(implicit ptt1:(ColumnTo[A1],ToStatement[A1]), ptt2:(ColumnTo[A2],ToStatement[A2]), r:Manifest[R])
    extends MagicParser2[A1,A2,R](table = table,columns = names)(ptt1._1,ptt2._1,r) with M2[A1,A2,R]{

        lazy val pt1= ptt1
        lazy val pt2= ptt2
    }

}