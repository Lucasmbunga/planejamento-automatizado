(define (problem rearrange-blocks)
  (:domain blocksworld)

  (:objects A B)

  (:init
    (on A B)
    (clear A)
    (onTable B)
    (handEmpty)
  )

  (:goal
    (and
      (on B A)
    )
  )
)