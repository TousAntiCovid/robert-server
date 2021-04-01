## CLEA coding guidelines

* Contributions are done through Merge Requests on the develop branch (gitflow). MR must be reviewed by another dev than
  the code author. a MR can be merged when approved by at least one reviewer.
* Development happens on feature branches.
* Commit often, small commits, and publish them on the central repositories to share the lastest developments with the
  dev team.
* Tests are run as part of the CI pipeline and should stay "green"
* Use Meaningful names (very important): classes, methods, variables
* Unit test the code
* Small methods
* Methods do one thing, only one level of abstraction
* DRY: Don't Repeat Yourself
* SOLID
    * S: Single-responsibility principle: a class should only have a single responsibility, that is, only changes to one
      part of the software's specification should be able to affect the specification of the class.
    * O: Open–closed principle: "software entities ... should be open for extension, but closed for modification."
    * L: Liskov substitution principle: "objects in a program should be replaceable with instances of their subtypes
      without altering the correctness of that program."
    * I: Interface segregation principle: "many client-specific interfaces are better than one general-purpose
      interface."
    * D: Dependency inversion principle: "depend upon abstractions, [not] concretions."
* Comments
    * Do not paraphrase the code. Make the code self-documentating.
    * Comment only what is not obvious by reading the code: informative comment, explanation of intent, warning. Use
      javadoc for public APIs.
* Formatting
    * Naming conventions: Camel case.
    * Unit of indentation is 4 spaces. No use of the tab character.
    * Curly braces at the end of the line that starts the class, method, loop, etc., and the closing brace is on a line
      by itself, lined up vertically with the start of the first line.
