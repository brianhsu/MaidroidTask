MaidroidTask
===================

Introduction
-----------------------

This is the core function library of a [GTD][1] software I would like to implement using the concept of [Clean Architecture][2] proposed by Robert C. Martin (Uncle Bob).

The main goal of this project, is to create a GTD-style TODO management software that could be run as both web application / android app or Java SWT desktop application or even as a console program.

Goals
--------------

During the devlopement of this system, I would like to test that I would able to achieve the following goals and test my skills as a sotware engineer.

  - Using the conepct of Clean Architecture to construct a software that could plug-in diffrent styles input output mechanism and UI.
  - TDD, for every production code, I should write test first.
  - BDD, for the use case objects, I should use the conecpt of BDD. And my test output will looks like specification instead of testing result.
  - Make test coverage rate I high as it could be. At least 90% statement coverage, 100% is even better.
  - It should be able to swith between any data storage system by simply implement correct interface.
  - It should be able to do DB read-write split by simply implement correct interface in our code.
  - It should be able to do DB sharding by simply implement correct interface in our code.
  - It should be able to sync the data between web application / Android application or even dekstop app.
  - The API server should able to handle C10K problem.

With these goals in mind, this is the core function library of this GTD system. Since it's a core library, it won't have any detail implementation (like DB or UI or even a API server) inside this repository. They should be belong to separate projects.

Build and Test.
------------------

This system is written is Scala and could be run on JVM. To build and run the test, download and install [SBT 1.3.x][3].

After that, simply run sbt test on the repository.

    $ cd MaidroidTask/
    $ sbt test                            # Test everything


[1]: https://en.wikipedia.org/wiki/Getting_Things_Done
[2]: https://www.youtube.com/watch?v=WpkDN78P884
[3]: https://www.scala-sbt.org/
