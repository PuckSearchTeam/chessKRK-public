# Comparison, 2026-11-05: A = decouple-iteration2.wld (the authors' second declaration), B = second-declarer/decouple.wld (the blind one)

## Axes

A: 17 axes, 78 packages placed; B: 23 axes, 78 packages placed; 78 placed in both of the 78.

Mapping of B's axes onto A's (packages shared):
- i18n (1): lowlevel 1
- taskmgmt (1): lowlevel 1
- configuration (1): lowlevel 1
- swingext (1): lowlevel 1
- model (1): model 1
- logging (1): lowlevel 1
- util (2): lowlevel 2
- gefext (1): gui 1
- kernel (1): kernel 1
- umlcommon (3): uml 3
- gui (3): gui 3
- explorer (2): explorer 2
- diagrams (17): diagrams 17
- proppanels (9): panels 9
- notation (5): notation 5
- codegen (3): codegen 2, gui 1
- reveng (2): reveng 2
- moduleloader (1): modules 1
- persistence (1): persistence 1
- profile (6): profile 6
- critics (10): cognitive 10
- ocl (1): ocl 1
- application (5): lowlevel 3, application 2

Partition agreement on the 78 packages placed by both: Rand index 0.981, adjusted Rand index 0.891.
Package pairs grouped by one and split by the other: 56 of 3003.

## Hidden pairs, without the optional block

Ordered package pairs (p must not use q): A hides 1621, B hides 1660, both 1376, A only 245, B only 284, neither 4101 of 6006.
Agreement 0.912, Cohen's kappa 0.778, Jaccard of the hidden sets 0.722.

## Hidden pairs, with the optional block

Ordered package pairs (p must not use q): A hides 3912, B hides 1952, both 1672, A only 2240, B only 280, neither 1814 of 6006.
Agreement 0.580, Cohen's kappa 0.241, Jaccard of the hidden sets 0.399.

## Violated uses, without the optional block

A: 894 lines, 849 distinct uses; B: 2473 lines, 2394 distinct uses; in both 822; Jaccard 0.340.
A per clause: hide model from lowlevel 27; hide kernel from lowlevel, model 0; hide gui from lowlevel, model 0; hide explorer from lowlevel, model 0; hide diagrams from lowlevel, model 0; hide panels from lowlevel, model 0; hide uml from lowlevel, model 0; hide notation from lowlevel, model 0; hide codegen from lowlevel, model 0; hide reveng from lowlevel, model 0; hide persistence from lowlevel, model 0; hide profile from lowlevel, model 0; hide modules from lowlevel, model 0; hide cognitive from lowlevel, model, kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 325; hide ocl from lowlevel, model, kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 16; hide application from lowlevel, model, kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules, cognitive, ocl 1; hide model, diagrams, uml, panels, cognitive from gui 525; containment between axes 0
B per clause: hide kernel from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide umlcommon from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide gui from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide explorer from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide diagrams from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide proppanels from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide notation from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide codegen from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide reveng from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide moduleloader from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide persistence from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide profile from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide critics from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 325; hide ocl from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 16; hide application from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile, critics, ocl 967; hide i18n from taskmgmt, configuration, swingext, model, logging 0; hide taskmgmt from i18n, configuration, swingext, model, logging 0; hide configuration from i18n, taskmgmt, swingext, model, logging 0; hide swingext from i18n, taskmgmt, configuration, model, logging 0; hide model from i18n, taskmgmt, configuration, swingext, logging 0; hide logging from i18n, taskmgmt, configuration, swingext, model 0; hide persistence from kernel 7; hide model, diagrams, critics from gui 376; hide proppanels, codegen, reveng, umlcommon from gui 149; hide targetmanager from all but-not-from gui, explorer 633; containment between axes 0

## Violated uses, with the optional block

A: 5090 lines, 4718 distinct uses; B: 2856 lines, 2762 distinct uses; in both 1734; Jaccard 0.302.
A per clause: hide model from lowlevel 27; hide kernel from lowlevel, model 0; hide gui from lowlevel, model 0; hide explorer from lowlevel, model 0; hide diagrams from lowlevel, model 0; hide panels from lowlevel, model 0; hide uml from lowlevel, model 0; hide notation from lowlevel, model 0; hide codegen from lowlevel, model 0; hide reveng from lowlevel, model 0; hide persistence from lowlevel, model 0; hide profile from lowlevel, model 0; hide modules from lowlevel, model 0; hide cognitive from lowlevel, model, kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 326; hide ocl from lowlevel, model, kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 16; hide application from lowlevel, model, kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules, cognitive, ocl 1; hide model, diagrams, uml, panels, cognitive from gui 527; hide kernel from gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 1158; hide gui from kernel, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 1295; hide explorer from kernel, gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 32; hide diagrams from kernel, gui, explorer, panels, uml, notation, codegen, reveng, persistence, profile, modules 554; hide panels from kernel, gui, explorer, diagrams, uml, notation, codegen, reveng, persistence, profile, modules 288; hide uml from kernel, gui, explorer, diagrams, panels, notation, codegen, reveng, persistence, profile, modules 133; hide notation from kernel, gui, explorer, diagrams, panels, uml, codegen, reveng, persistence, profile, modules 402; hide codegen from kernel, gui, explorer, diagrams, panels, uml, notation, reveng, persistence, profile, modules 23; hide reveng from kernel, gui, explorer, diagrams, panels, uml, notation, codegen, persistence, profile, modules 33; hide persistence from kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, profile, modules 94; hide profile from kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, modules 170; hide modules from kernel, gui, explorer, diagrams, panels, uml, notation, codegen, reveng, persistence, profile 11; containment between axes 0
B per clause: hide kernel from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide umlcommon from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide gui from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel 3; hide explorer from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel 0; hide diagrams from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, notation 123; hide proppanels from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon 0; hide notation from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel 112; hide codegen from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon 0; hide reveng from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon 0; hide moduleloader from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide persistence from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide profile from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide critics from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 326; hide ocl from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 16; hide application from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile, critics, ocl 967; hide i18n from taskmgmt, configuration, swingext, model, logging 0; hide taskmgmt from i18n, configuration, swingext, model, logging 0; hide configuration from i18n, taskmgmt, swingext, model, logging 0; hide swingext from i18n, taskmgmt, configuration, model, logging 0; hide model from i18n, taskmgmt, configuration, swingext, logging 0; hide logging from i18n, taskmgmt, configuration, swingext, model 0; hide persistence from kernel 7; hide model, diagrams, critics from gui 378; hide proppanels, codegen, reveng, umlcommon from gui 149; hide targetmanager from all but-not-from gui, explorer 633; hide notation, profile from gui 88; hide reveng from all but-not-from proppanels 8; hide util from gefext, i18n, taskmgmt, configuration, swingext, model, logging 0; hide gefext from util, i18n, taskmgmt, configuration, swingext, model, logging 0; hide i18n from util, gefext, taskmgmt, configuration, swingext, model, logging 31; hide taskmgmt from util, gefext, i18n, configuration, swingext, model, logging 0; hide configuration from util, gefext, i18n, taskmgmt, swingext, model, logging 8; hide swingext from util, gefext, i18n, taskmgmt, configuration, model, logging 0; hide model from util, gefext, i18n, taskmgmt, configuration, swingext, logging 7; hide logging from util, gefext, i18n, taskmgmt, configuration, swingext, model 0; containment between axes 0


# A = decouple.wld (the authors' first declaration, before its one revision), B = the blind one

## Axes

A: 16 axes, 78 packages placed; B: 23 axes, 78 packages placed; 78 placed in both of the 78.

Mapping of B's axes onto A's (packages shared):
- i18n (1): lowlevel 1
- taskmgmt (1): lowlevel 1
- configuration (1): lowlevel 1
- swingext (1): lowlevel 1
- model (1): model 1
- logging (1): lowlevel 1
- util (2): lowlevel 2
- gefext (1): gui 1
- kernel (1): kernel 1
- umlcommon (3): uml 3
- gui (3): gui 3
- explorer (2): gui 2
- diagrams (17): diagrams 17
- proppanels (9): panels 9
- notation (5): notation 5
- codegen (3): codegen 2, gui 1
- reveng (2): reveng 2
- moduleloader (1): modules 1
- persistence (1): persistence 1
- profile (6): profile 6
- critics (10): cognitive 10
- ocl (1): ocl 1
- application (5): application 5

Partition agreement on the 78 packages placed by both: Rand index 0.987, adjusted Rand index 0.924.
Package pairs grouped by one and split by the other: 39 of 3003.

## Hidden pairs, without the optional block

Ordered package pairs (p must not use q): A hides 1749, B hides 1660, both 1596, A only 153, B only 64, neither 4193 of 6006.
Agreement 0.964, Cohen's kappa 0.911, Jaccard of the hidden sets 0.880.

## Hidden pairs, with the optional block

Ordered package pairs (p must not use q): A hides 3856, B hides 1952, both 1888, A only 1968, B only 64, neither 2086 of 6006.
Agreement 0.662, Cohen's kappa 0.385, Jaccard of the hidden sets 0.482.

## Violated uses, without the optional block

A: 2890 lines, 2829 distinct uses; B: 2473 lines, 2394 distinct uses; in both 1797; Jaccard 0.525.
A per clause: hide model from lowlevel 7; hide kernel from lowlevel, model 0; hide gui from lowlevel, model, kernel 3; hide diagrams from lowlevel, model, kernel 118; hide panels from lowlevel, model, kernel 0; hide uml from lowlevel, model, kernel 8; hide notation from lowlevel, model, kernel 112; hide codegen from lowlevel, model, kernel 0; hide reveng from lowlevel, model, kernel 0; hide persistence from lowlevel, model, kernel 7; hide profile from lowlevel, model, kernel 57; hide modules from lowlevel, model, kernel 0; hide cognitive from lowlevel, model, kernel, gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 325; hide ocl from lowlevel, model, kernel, gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 16; hide application from lowlevel, model, kernel, gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules, cognitive, ocl 967; hide model, diagrams, uml, panels, cognitive from gui 1270; containment between axes 0
B per clause: hide kernel from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide umlcommon from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide gui from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide explorer from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide diagrams from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide proppanels from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide notation from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide codegen from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide reveng from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide moduleloader from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide persistence from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide profile from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide critics from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 325; hide ocl from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 16; hide application from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile, critics, ocl 967; hide i18n from taskmgmt, configuration, swingext, model, logging 0; hide taskmgmt from i18n, configuration, swingext, model, logging 0; hide configuration from i18n, taskmgmt, swingext, model, logging 0; hide swingext from i18n, taskmgmt, configuration, model, logging 0; hide model from i18n, taskmgmt, configuration, swingext, logging 0; hide logging from i18n, taskmgmt, configuration, swingext, model 0; hide persistence from kernel 7; hide model, diagrams, critics from gui 376; hide proppanels, codegen, reveng, umlcommon from gui 149; hide targetmanager from all but-not-from gui, explorer 633; containment between axes 0

## Violated uses, with the optional block

A: 5529 lines, 5068 distinct uses; B: 2856 lines, 2762 distinct uses; in both 2699; Jaccard 0.526.
A per clause: hide model from lowlevel 7; hide kernel from lowlevel, model 0; hide gui from lowlevel, model, kernel 3; hide diagrams from lowlevel, model, kernel 118; hide panels from lowlevel, model, kernel 0; hide uml from lowlevel, model, kernel 8; hide notation from lowlevel, model, kernel 112; hide codegen from lowlevel, model, kernel 0; hide reveng from lowlevel, model, kernel 0; hide persistence from lowlevel, model, kernel 7; hide profile from lowlevel, model, kernel 57; hide modules from lowlevel, model, kernel 0; hide cognitive from lowlevel, model, kernel, gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 326; hide ocl from lowlevel, model, kernel, gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 16; hide application from lowlevel, model, kernel, gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules, cognitive, ocl 967; hide model, diagrams, uml, panels, cognitive from gui 1273; hide gui from diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules 1229; hide diagrams from gui, panels, uml, notation, codegen, reveng, persistence, profile, modules 436; hide panels from gui, diagrams, uml, notation, codegen, reveng, persistence, profile, modules 288; hide uml from gui, diagrams, panels, notation, codegen, reveng, persistence, profile, modules 125; hide notation from gui, diagrams, panels, uml, codegen, reveng, persistence, profile, modules 290; hide codegen from gui, diagrams, panels, uml, notation, reveng, persistence, profile, modules 23; hide reveng from gui, diagrams, panels, uml, notation, codegen, persistence, profile, modules 33; hide persistence from gui, diagrams, panels, uml, notation, codegen, reveng, profile, modules 87; hide profile from gui, diagrams, panels, uml, notation, codegen, reveng, persistence, modules 113; hide modules from gui, diagrams, panels, uml, notation, codegen, reveng, persistence, profile 11; containment between axes 0
B per clause: hide kernel from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide umlcommon from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide gui from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel 3; hide explorer from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel 0; hide diagrams from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, notation 123; hide proppanels from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon 0; hide notation from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel 112; hide codegen from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon 0; hide reveng from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon 0; hide moduleloader from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide persistence from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide profile from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext 0; hide critics from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 326; hide ocl from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile 16; hide application from i18n, taskmgmt, configuration, swingext, model, logging, util, gefext, kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile, critics, ocl 967; hide i18n from taskmgmt, configuration, swingext, model, logging 0; hide taskmgmt from i18n, configuration, swingext, model, logging 0; hide configuration from i18n, taskmgmt, swingext, model, logging 0; hide swingext from i18n, taskmgmt, configuration, model, logging 0; hide model from i18n, taskmgmt, configuration, swingext, logging 0; hide logging from i18n, taskmgmt, configuration, swingext, model 0; hide persistence from kernel 7; hide model, diagrams, critics from gui 378; hide proppanels, codegen, reveng, umlcommon from gui 149; hide targetmanager from all but-not-from gui, explorer 633; hide notation, profile from gui 88; hide reveng from all but-not-from proppanels 8; hide util from gefext, i18n, taskmgmt, configuration, swingext, model, logging 0; hide gefext from util, i18n, taskmgmt, configuration, swingext, model, logging 0; hide i18n from util, gefext, taskmgmt, configuration, swingext, model, logging 31; hide taskmgmt from util, gefext, i18n, configuration, swingext, model, logging 0; hide configuration from util, gefext, i18n, taskmgmt, swingext, model, logging 8; hide swingext from util, gefext, i18n, taskmgmt, configuration, model, logging 0; hide model from util, gefext, i18n, taskmgmt, configuration, swingext, logging 7; hide logging from util, gefext, i18n, taskmgmt, configuration, swingext, model 0; containment between axes 0
