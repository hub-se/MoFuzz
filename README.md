# MoFuzz: A Fuzzer Suite for Testing Model-Driven Software Engineering Tools

This repository provides the implementation and evaluation subjects for the paper *MoFuzz: A Fuzzer Suite for Testing Model-Driven Software Engineering Tools* accepted for the research track of ASE'2020.

MoFuzz utilizes coverage-guided fuzzing and automated model generation to test Model-Driven Software Engineering (MDSE) tools. 

Authors:
[Hoang Lam Nguyen](https://www.informatik.hu-berlin.de/en/Members/hoang-lam-nguyen), [Nebras Nassar](https://www.uni-marburg.de/fb12/arbeitsgruppen/swt/nebras-nassar), [Timo Kehrer](https://www.informatik.hu-berlin.de/de/forschung/gebiete/mse/mitarb/kehrerti.html), and [Lars Grunske](https://www.informatik.hu-berlin.de/de/Members/lars-grunske)

## Installation

MoFuzz is built on top of [JQF](https://github.com/rohanpadhye/jqf): a feedback-directed fuzz testing platform for Java.
We provide instructions to install and run MoFuzz locally or inside a Docker container:

### Setup locally
#### Requirements
* Git, Maven
* Java JDK = 1.8

1. Clone repository:
```
git clone https://github.com/hub-se/MoFuzz.git
```

2. Build MoFuzz
```
cd MoFuzz/mofuzz
mvn package
cd ..
```

### Setup as Docker container
#### Requirements
* Docker

1. Clone repository:
```
git clone https://github.com/hub-se/MoFuzz.git
```

2. Build container:
```
cd MoFuzz
docker build -t mofuzz .
```

3. Run container:
```
docker run -dt --name=mofuzz-container mofuzz
docker exec -it mofuzz-container /bin/bash
```

## Running MoFuzz
After finishing the setup as described above, MoFuzz can be executed using one of the scripts inside the [scripts/](https://github.com/hub-se/MoFuzz/tree/master/scripts) folder. The following input generation strategies are available (for detailed descriptions please check out the paper):

1. **MoFuzz-emf-modelgen** (scripts/mofuzz-emf-modelgen.sh): black-box, graph-grammar based
2. **MoFuzz-cgf-emfedit** (scripts/mofuzz-cgf-emfedit.sh): coverage-guided, mutation-based
3. **MoFuzz-cgf-cpeo** (scripts/mofuzz-cgf-cpeo.sh): coverage-guided, rule-based
4. **Random** (scripts/random_instantiator.sh): random, containment-tree based
5. **Zest** (scripts/zest.sh): coverage-guided, containment-tree based

The scripts are used as follows:
```
./scripts/selected_strategy.sh TEST_CLASS TEST_METHOD
```

We provide the following subjects from the original evaluation of MoFuzz:

| Name         | TEST_CLASS                | TEST_METHOD         |
|--------------|---------------------------|---------------------|
| UML2Java     | AcceleoUML2JavaHeliosTest | simpleGeneratorTest |
| EcoreUtil    | EcoreUtilsTest            | completeTest        |
| EMFCompare   | EMFCompareTest            | diffTest            |
| UMLValidator | UML2ValidatorTest         | test                |
| UML2OWL      | UML2OWLTest               | test                |
| EMF2GraphViz | EMF2GraphvizTest          | test                |

For example, running MoFuzz using the random strategy on the EcoreUtil subject results in the following command:
```
./scripts/random_instantiator.sh EcoreUtilsTest completeTest
```

After some initialization time, the output should look like this:
```
Coverage-guided Modelfuzzing
--------------------------

Test name:            fr.inria.atlanmod.instantiator.benchmarks.EcoreUtilsTest#completeTest
Results directory:    /workspace/MoFuzz/evaluation/results/random_instantiator/EcoreUtilsTest_completeTest
Elapsed time:         20s (no time limit)
Number of executions: 82
Valid inputs:         82 (100.00%)
Cycles completed:     0
Unique failures:      0
Queue size:           0
Current parent input: <seed>
Execution speed:      6/sec now | 3/sec overall
Total coverage:       1,211 branches (1.85% of map)
```
Running MoFuzz this way results in an infinite fuzzing loop and must be manually aborted (CTRL+C).

To prevent this, a timeout can be specified using the ```timeout``` command:
```
timeout 3600s ./scripts/mofuzz-emf-modelgen.sh UML2ValidatorTest test
```

Detailed evaluation results (log data and coverage stats over time) can be found in the following subdirectory: ```evaluation/results```.
