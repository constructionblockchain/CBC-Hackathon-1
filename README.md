# CBC Hackathon 1

## Overview

This CorDapp automates the following construction workflow:

* A developer and a contractor agree on a job composed of milestones
* For each of the job's milestones:

    * The contractor starts work on the milestone
    * The contractor completes work on the milestone
    * The developer inspects the work on the milestone
    * The developer accepts the work on the milestone
        * The developer can also reject the work on the milestone, in which case 
          the contractor must continue work on the milestone
    * The developer pays the contractor for the work on the milestone
    
## Running the CorDapp

* Deploy a test network using the `deployNodes` Gradle task
* Run the test network using the `build/nodes/runnodes` script
* Run the webservers using either the `runPartyAServer` and `runPartyBServer` Gradle tasks, or the 
  `Run PartyA Server - Port 10020` and `Run PartyB Server - Port 10021` run configurations
* Run flows using the endpoints defined in `FlowController`
* View states using the endpoints defined in `StateController`

## TODOs

* Subjobs/splitting of milestones into tasks
* Percentage completion and payment
* Retentions of 5%/3% (allow user to specify) per milestone that are paid once all milestones are complete
    * Of which 2.5% is paid one year later
* Map descriptions to BIM XML
* Architectural drawings as a property
* Milestone deadlines
* Mobilisation fee
* Allow contractor to reject job
* Include other legal documents such as tender etc when proposing a job
* Allow milestone to be added, but
    * Not after final milestone has been completed
    * Not at an earlier stage than the latest completed milestone
* Allow unfinished milestones to be modified
* Provide total amount for project, and check milestones values don't exceed this
* Provide status for overall job so job can be started separately of milestones
* Capture individual, not just node, who started/reviewed/etc. milestone
* Include documents as attachments when completing a job