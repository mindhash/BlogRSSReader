package com.linkscraper

import akka.actor.{Actor, ActorLogging, ActorRef,Props}
import java.net.URL
import scala.xml._
import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Locale, Date}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global 
import akka.event.Logging
import akka.pattern.ask
import scala.concurrent._
import scala.util.{Try, Success, Failure}
import java.io._

import com.linkscraper.models._

abstract class Reader extends Actor { 

  def print(feed:RssFeed) {
    println(feed.latest)
  }
}

class AtomReader extends Reader {

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);

  private def parseAtomDate(date:String, formatter:SimpleDateFormat):Date = {
    val newDate = date.reverse.replaceFirst(":", "").reverse
    return formatter.parse(newDate)
  }

  private def getHtmlLink(node:NodeSeq) = {
    node
      .filter(n => (n \ "@type").text == "text/html")
      .map( n => (n \ "@href").text).head
  }

  def extract(xml:Elem) : Seq[RssFeed] = {
    for (feed <- xml \\ "feed") yield {
      val items = for (item <- (feed \\ "entry")) yield {
        RssItem(
          (item \\ "title").text,
          getHtmlLink((item \\ "link")),
          (item \\ "summary").text,
          parseAtomDate((item \\ "published").text, dateFormatter),
          (item \\ "id").text
        )
      }
      AtomRssFeed(
        (feed \ "title").text,
        getHtmlLink((feed \ "link")),
        (feed \ "subtitle ").text,
        items.take(8))
    }
  }

  def receive() = {
    case xml:Elem => {
      extract(xml) match {
        case head :: tail => print(head)
        case Nil =>
      }
    }
  }
}


class XmlReader extends Reader {

  val dateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

  def extract(xml:Elem) : Seq[RssFeed] = {

    for (channel <- xml \\ "channel") yield {
      val items = for (item <- (channel \\ "item")) yield {
        RssItem(
          (item \\ "title").text,
          (item \\ "link").text,
          (item \\ "description").text,
          dateFormatter.parse((item \\ "pubDate").text),
          (item \\ "guid").text
        )
      }
      XmlRssFeed(
        (channel \ "title").text,
        (channel \ "link").text,
        (channel \ "description").text,
        (channel \ "language").text,
        items.take(8))
    }
  }

  def receive() = {
    case xml:Elem => {
       
 		sender() !  RssReader.RssFeedData( extract(xml)) /*match {
        case head :: tail => sender() ! feed //print(head) 
        case Nil => print("Could not parse Feed ")
      }*/


    }
  }
}


class RssReader extends Actor with ActorLogging {

 var replyTo: ActorRef = _


 def read(url : URL) = {
    Try(url.openConnection.getInputStream) match {
      case Success(u) => {
        val xml = XML.load(u)
        implicit val timeout = Timeout(30.seconds)
        val actor = if((xml \\ "channel").length == 0) context.actorOf(Props[AtomReader])
                    else context.actorOf(Props[XmlReader])
        actor ! xml
      }
      case Failure(_) =>
    }
  }

	def receive ={
		case path: String => 
			println("Fetching Article List" + path)
			replyTo = sender
			val url: URL = new URL(path)
			read(url)
		case RssReader.RssFeedData (feed: Seq[RssFeed])=>
			println("got feed data")
			//send back to linkworker
			replyTo  ! 	RssReader.RssFeedData(feed)
	}

}


object RssReader {
	val props = Props[RssReader]
	case class RssFeedData (feed: Seq[RssFeed])
}