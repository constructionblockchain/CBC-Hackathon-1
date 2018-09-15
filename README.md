# CBC Hackathon 1

A CorDapp automating the following construction workflow:

* A developer and a contractor agree on a job composed of milestones
* For each of the job's milestones:

    * The contractor starts work on the milestone
    * The contractor completes work on the milestone
    * The developer inspects the work on the milestone
    * The developer accepts the work on the milestone
        * The developer can also reject the work on the milestone, in which case 
          the contractor must continue work on the milestone
    * The developer pays the contractor for the work on the milestone

## TODOs

* Subjobs
* Percentage completion and payment
* Retentions of 5% per milestone that are paid once all milestones are complete
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