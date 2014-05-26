Data
====

A few data utilities I have written over the years.

at.yawk.data.xml.lexer
----------------------

An easy-to-use (but not particularly fast) XML lexer.

```Java
BlockingLexer lexer = new BlockingLexer();
// typical XML shenanigans, tagsoup works well here
lexer.digest(new org.ccil.cowan.tagsoup.Parser(), yourInputSource);
// OR
lexer.digestParallel(new org.ccil.cowan.tagsoup.Parser(), yourInputSource, yourThreadPool);

// print "Hello World" for input "<a href='http://yawk.at/'>Hello World</a>"

// skip to first 'a' tag with the attribute href="http://yawk.at/"
lexer.start("a", "href", "http://yawk.at/");
// jump to the next element
lexer.next();
// print the text of the current element (we assume it's actually text, otherwise this would throw)
System.out.println(lexer.getText());
```
