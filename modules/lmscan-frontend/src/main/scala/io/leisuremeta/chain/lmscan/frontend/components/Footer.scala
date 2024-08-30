package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Footer:
  def view(): Html[Msg] =
    div(
      div(cls := "footer-left")(
        p(
          strong("LMSCAN"),
          span("for LeisureMeta Chain")
        ),
       strong("Copyright © LM LLC All Rights Reserved")
      ),
      div(cls := "footer-right")(
        dl(
          dt("Company Family"),
          dd(
            a(
              href := "https://themoonlabs.net/",
              target :="_blank",
              cls := "icon-The-Moon-Labs"
            )("The Moon Labs"),
            a(
              href := "https://leisuremeta.io/",
              target :="_blank",
              cls := "icon-leisuremetaverse"
            )("LeisureMetaverse"),
            a(
              href := "https://www.ilikelm.com/",
              target :="_blank",
              cls := "icon-lm-nova"
            )("ILIKELM"),
            a(
              href := "https://www.themoonent.com/",
              target :="_blank",
              cls := "icon-tme"
            )("THE MOON ENTERTAINMENT"),
          )
        ),
        dl(
          dt("Social"),
          dd(
            a(
              href := "https://github.com/leisuremeta/leisuremeta-chain",
              target :="_blank",
              cls := "icon-github"
            )("Github"),
            a(
              href := "https://t.me/LeisureMeta_Official",
              target :="_blank",
              cls := "icon-telegram"
            )("Telegram"),
            a(
              href := "https://twitter.com/LeisureMeta_LM",
              target :="_blank",
              cls := "icon-twitter"
            )("Twitter"),
            a(
              href := "https://discord.com/invite/5ttwp2dzEr",
              target :="_blank",
              cls := "icon-discord"
            )("Discord"),
          )
        ),
      ),
    )
