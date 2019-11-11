(ns namr.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [namr.core-test]))

(doo-tests 'namr.core-test)

