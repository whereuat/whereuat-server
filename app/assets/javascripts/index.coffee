document.addEventListener "DOMContentLoaded", (event) ->
  for card in document.getElementsByClassName "card-container"
    card.addEventListener "click", ->
      if "flip" in @.classList
        @.classList.remove "flip"
      else
        @.classList.add "flip"
