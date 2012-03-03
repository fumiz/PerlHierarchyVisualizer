PerlHierarchyVisualizer
======================

This scala code extract `use base` from .pm files and create SIF format text

Environment
------
scala-2.9.1

Usage
------
    % scalac PerlHierarchyVisualizer.scala
    % scala PerlHierarchyVisualizer ~/perl5/perlbrew/perls/perl-5.14.2/lib/site_perl/5.14.2/darwin-2level/DBD > ~/dbd.sif

Example
------
![画像1](https://raw.github.com/fumiz/PerlHierarchyVisualizer/doc/example.png "diagram example")

visualized by [Cytoscape](http://www.cytoscape.org/)

