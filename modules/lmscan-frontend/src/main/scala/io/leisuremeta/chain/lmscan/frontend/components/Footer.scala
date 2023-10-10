package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Footer:
  def view(): Html[Msg] =
    div(`class` := "footer")(
      div(`class` := "footer-left")(
        div(`class` := "top-box")(
          p(
            strong("LMSCAN"),
            span(" | LEISUREMETAVERSE")
          ),
          p("A block explorer for LeisureMeta Chain")
        ),
        p(`class` := "copyright")("Copyright &copy; LM LLC All Rights Reserved")
      ),
      div(`class` := "footer-right")(
        dl(`class` := "family-lick")(
          dt("Company Family"),
          dd(
            ul(
              li(
                a(
                  href := "https://themoonlabs.net/",
                  target :="_blank",
                  `class` := "icon-The-Moon-Labs"
                )("The Moon Labs")
              ),
              li(
                a(
                  href := "https://leisuremeta.io/",
                  target :="_blank",
                  `class` := "icon-leisuremetaverse"
                )("LeisureMetaverse")
              ),
              li(
                a(
                  href := "https://m.playnomm.com/",
                  target :="_blank",
                  `class` := "icon-playnomm"
                )("playNomm")
              ),
              li(
                a(
                  href := "https://www.ilikelm.com/",
                  target :="_blank",
                  `class` := "icon-lm-nova"
                )("LM NOVA")
              ),
            )
          )
        ),
        dl(`class` := "social-lick")(
          dt("Social"),
          dd(
            ul(
              li(
                a(
                  href := "https://github.com/leisuremeta/leisuremeta-chain",
                  target :="_blank",
                  `class` := "icon-github"
                )("Github")
              ),
              li(
                a(
                  href := "https://t.me/LeisureMeta_Official",
                  target :="_blank",
                  `class` := "icon-telegram"
                )("Telegram")
              ),
              li(
                a(
                  href := "https://twitter.com/LeisureMeta_LM",
                  target :="_blank",
                  `class` := "icon-twitter"
                )("Twitter")
              ),
              li(
                a(
                  href := "https://discord.com/invite/5ttwp2dzEr",
                  target :="_blank",
                  `class` := "icon-discord"
                )("Discord")
              )
            )
          )
        ),
      ),
    )
