package swave.rsc

import java.util.function.{ Function ⇒ JFunction, Predicate ⇒ JPredicate, BiPredicate }

object Java8Compat {
  implicit def toJavaFunction[A, B](f: A ⇒ B): JFunction[A, B] = new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  implicit def toJavaPredicate[A](f: A ⇒ Boolean): JPredicate[A] = new JPredicate[A] {
    override def test(a: A): Boolean = f(a)
  }

  implicit def toJavaBiPredicate[A, B](predicate: (A, B) ⇒ Boolean): BiPredicate[A, B] =
    new BiPredicate[A, B] {
      def test(a: A, b: B) = predicate(a, b)
    }
}
