(ns hello-world.macros)

(defmacro mult-macro [a b]
    `(* ~a ~b))

(prn "prn from inside hello-world.macros")
