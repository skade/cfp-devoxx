/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Association du Paris Java User Group.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package controllers

import models._
import play.api.mvc._
import akka.util.Crypt
import library.{LogURL, ZapActor}
import play.api.libs.json.Json

/**
 * Simple content publisher
 * Created by nicolas on 12/02/2014.
 */
object Publisher extends Controller {
  def homePublisher = Action {
    implicit request =>
      val result=views.html.Publisher.homePublisher()
      val etag = Crypt.md5(result.toString()).toString
      val maybeETag = request.headers.get(IF_NONE_MATCH)

      maybeETag match {
        case Some(oldEtag) if oldEtag == etag => NotModified
        case other=>Ok(result).withHeaders(ETAG -> etag)
      }
  }

  def showAllSpeakers = Action {
    implicit request =>
      val speakers = Speaker.allSpeakersWithAcceptedTerms()
      val etag = speakers.hashCode().toString+"_2"
      val maybeETag = request.headers.get(IF_NONE_MATCH)
      maybeETag match {
        case Some(oldEtag) if oldEtag == etag => NotModified
        case other => Ok(views.html.Publisher.showAllSpeakers(speakers)).withHeaders(ETAG -> etag)
      }
  }

  def showSpeaker(uuid: String, name: String) = Action {
    implicit request =>
      val maybeSpeaker=Speaker.findByUUID(uuid)
      maybeSpeaker match {
        case Some(speaker)=>{
          val acceptedProposals = ApprovedProposal.allAcceptedTalksForSpeaker(speaker.uuid)
          ZapActor.actor ! LogURL("showSpeaker",uuid,name)
          Ok(views.html.Publisher.showSpeaker(speaker,acceptedProposals))
        }
        case None=>NotFound("Speaker not found")
      }
  }

  def showByTalkType(talkType:String)=Action{
    implicit request=>
      val proposals=Proposal.allAcceptedByTalkType(talkType)
      Ok(views.html.Publisher.showByTalkType(proposals,talkType))
  }

  def showDetailsForProposal(proposalId:String, proposalTitle:String)=Action{
    implicit request=>
      Proposal.findById(proposalId) match {
        case None=>NotFound("Proposal not found")
        case Some(proposal)=>
          ZapActor.actor ! LogURL("showTalk",proposalId, proposalTitle)
          Ok(views.html.Publisher.showProposal(proposal))
      }
  }

  def showAgendaByConfType(confType:String,slotId:String)=Action{
    implicit request=>
      val maybeScheduledConfiguration = ScheduleConfiguration.loadScheduledConfiguration(slotId)
      maybeScheduledConfiguration match{
        case Some(slots)=>Ok(views.html.Publisher.showAgendaByConfType(slots,confType))
        case None=>NotFound
      }
  }

  def showByDay(day:String)=Action{
     implicit request=>
    day match {
      case "mercredi" => Ok(views.html.Publisher.showWednesday())
      case "wednesday" => Ok(views.html.Publisher.showWednesday())
      case ProposalType.LAB.id => Ok(views.html.Publisher.showWednesday())
      case ProposalType.TIA.id => Ok(views.html.Publisher.showWednesday())
      case ProposalType.UNI.id => Ok(views.html.Publisher.showWednesday())

      case "jeudi" => Ok(views.html.Publisher.showThursday())
      case "thursday" => Ok(views.html.Publisher.showThursday())
      case "confThursday" => Ok(views.html.Publisher.showThursday())
      case "quickThursday" => Ok(views.html.Publisher.showThursday())
      case ProposalType.BOF.id => Ok(views.html.Publisher.showThursday())

      case "vendredi" => Ok(views.html.Publisher.showFriday())
      case "friday" => Ok(views.html.Publisher.showFriday())
      case "confFriday" => Ok(views.html.Publisher.showFriday())
      case "quickFriday" => Ok(views.html.Publisher.showFriday())

      case other => NotFound
    }
  }
}
