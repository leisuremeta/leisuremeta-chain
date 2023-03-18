package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object BoardView:
  val view = (model: Model) =>
    div(`class` := "table-area")(
      div(`class` := "font-40px pt-16px font-block-detail color-white")(
        "Transactions",
      ),
      div(`class` := "table-list x", id := "oop-table-blocks")(
        div(`class` := "table-container")(
          div(`class` := "table w-[100%]")(
            div(`class` := "row table-head")(
              div(`class` := "cell")(span("pages")),
              div(`class` := "cell")(span("Block")),
              div(`class` := "cell")(span("Age")),
              div(`class` := "cell")(span("Signer")),
              div(`class` := "cell")(span("Value")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "605a36d218fb925ebc673d7faa422138119cb23bd0f02333bb8428348e4f8c0b",
                  ),
                )("605a36d218..."),
              ),
              div(`class` := "cell")(span("1414443")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-16 00:27:09"))(
                  "8 mins ago",
                ),
              ),
              div(`class` := "cell type-3")(span("9c2418c620...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "449697ccc18b8f8fc742d746a27ece666d4d90f5d417a106d93429d47a7d88f9",
                  ),
                )("449697ccc1..."),
              ),
              div(`class` := "cell")(span("1414442")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-16 00:27:07"))(
                  "8 mins ago",
                ),
              ),
              div(`class` := "cell type-3")(span("010cd45939...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "2fb09c8a1a76c52ba4db87b0c51faf83338f7def403f6e91149490654b698928",
                  ),
                )("2fb09c8a1a..."),
              ),
              div(`class` := "cell")(span("1414441")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 23:51:35"))(
                  "44 mins ago",
                ),
              ),
              div(`class` := "cell type-3")(span("010cd45939...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "38a24b83e437f83e91882238fc2d2a29ff5929d529f16a261618821cbe9f99ee",
                  ),
                )("38a24b83e4..."),
              ),
              div(`class` := "cell")(span("1414440")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 23:24:55"))(
                  "1 hour ago",
                ),
              ),
              div(`class` := "cell type-3")(span("010cd45939...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "2b45aef4941a54d3d9cf809445ca22f14b1c8bf67e72abde7a4aa2064365b431",
                  ),
                )("2b45aef494..."),
              ),
              div(`class` := "cell")(span("1414439")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 23:24:50"))(
                  "1 hour ago",
                ),
              ),
              div(`class` := "cell type-3")(span("18c5d9d0ad...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "c76ec73a272e938922032c5fc5b5b33ff9811cfb8161e9ccd98b1d237b2a75a6",
                  ),
                )("c76ec73a27..."),
              ),
              div(`class` := "cell")(span("1414438")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 23:09:08"))(
                  "1 hour ago",
                ),
              ),
              div(`class` := "cell type-3")(span("1cc77f5767...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "98ad9bfe10e15483b92263ce19622b0099fd316cb1843fc870b06bdf44481770",
                  ),
                )("98ad9bfe10..."),
              ),
              div(`class` := "cell")(span("1414437")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 23:09:03"))(
                  "1 hour ago",
                ),
              ),
              div(`class` := "cell type-3")(span("010cd45939...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "edb554b09acc61aecf1be50b1bfa8ce7eb1be4470f7c61395677b92a31652349",
                  ),
                )("edb554b09a..."),
              ),
              div(`class` := "cell")(span("1414436")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 22:53:50"))(
                  "1 hour ago",
                ),
              ),
              div(`class` := "cell type-3")(span("010cd45939...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "f2d4090d036224403eee886d5bd6caf3fb5b6e49a392a261d4a98fc7fc3bdd43",
                  ),
                )("f2d4090d03..."),
              ),
              div(`class` := "cell")(span("1414435")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 22:53:46"))(
                  "1 hour ago",
                ),
              ),
              div(`class` := "cell type-3")(span("1cc77f5767...")),
              div(`class` := "cell")(span("-")),
            ),
            div(`class` := "row table-body")(
              div(`class` := "cell type-3")(
                span(
                  Attribute(
                    "data-tooltip-text",
                    "cc9884026ccdbe9ed9db8455793a877f72d8f2c6f40c71616bb318c78482a483",
                  ),
                )("cc9884026c..."),
              ),
              div(`class` := "cell")(span("1414434")),
              div(`class` := "cell")(
                span(Attribute("data-tooltip-text", "2023-03-15 22:20:15"))(
                  "2 hours ago",
                ),
              ),
              div(`class` := "cell type-3")(span("0e4ec2fffd...")),
              div(`class` := "cell")(span("-")),
            ),
          ),
          div(
            `class` := "state DashBoard _search false table-search xy-center",
          )(
            div(`class` := "xy-center")(
              div(`class` := "type-arrow", style := "color:lightgray;")("<<"),
              div(`class` := "type-arrow", style := "color:lightgray;")("<"),
              div(`class` := "type-text-btn")(
                span(`class` := "selectedPage")("1"),
                span(`class` := "")("2"),
                span(`class` := "")("3"),
                span(`class` := "")("4"),
                span(`class` := "")("5"),
              ),
              div(`class` := "type-arrow")(">"),
              div(`class` := "type-arrow")(">>"),
              div(style := "margin-left:10px;")(
                input(
                  `class` := "type-search xy-center DOM-page1 margin-right text-center",
                ),
                div(`class` := "type-plain-text margin-right")("of"),
                div(`class` := "type-plain-text margin-right")("142588"),
              ),
            ),
          ),
        ),
      ),
    )
