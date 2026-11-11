(define (domain blocksworld)
  (:requirements :strips)

  (:predicates
    (on ?x ?y)
    (onTable ?x)
    (clear ?x)
    (handEmpty)
    (holding ?x)
  )

  (:action unstack
    :parameters (?x ?y)
    :precondition
      (and
        (on ?x ?y)
        (clear ?x)
        (handEmpty)
      )
    :effect
      (and
        (holding ?x)
        (clear ?y)
        (not (on ?x ?y))
        (not (clear ?x))
        (not (handEmpty))
      )
  )

  (:action putdown
    :parameters (?x)
    :precondition
      (holding ?x)
    :effect
      (and
        (onTable ?x)
        (clear ?x)
        (handEmpty)
        (not (holding ?x))
      )
  )

  (:action pickup
    :parameters (?x)
    :precondition
      (and
        (onTable ?x)
        (clear ?x)
        (handEmpty)
      )
    :effect
      (and
        (holding ?x)
        (not (onTable ?x))
        (not (clear ?x))
        (not (handEmpty))
      )
  )

  (:action stack
    :parameters (?x ?y)
    :precondition
      (and
        (holding ?x)
        (clear ?y)
      )
    :effect
      (and
        (on ?x ?y)
        (clear ?x)
        (handEmpty)
        (not (holding ?x))
        (not (clear ?y))
      )
  )
)