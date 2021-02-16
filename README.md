# DataHub: A Generalized Metadata Search & Discovery Tool
[![Version](https://img.shields.io/github/v/release/linkedin/datahub?include_prereleases)](https://github.com/linkedin/datahub/releases)
[![build & test](https://github.com/linkedin/datahub/workflows/build%20&%20test/badge.svg?branch=master&event=push)](https://github.com/linkedin/datahub/actions?query=workflow%3A%22build+%26+test%22+branch%3Amaster+event%3Apush)
[![Docker Pulls](https://img.shields.io/docker/pulls/linkedin/datahub-gms.svg)](https://hub.docker.com/r/linkedin/datahub-gms)
[![Get on Slack](https://img.shields.io/badge/slack-join-orange.svg)](https://join.slack.com/t/datahubspace/shared_invite/zt-dkzbxfck-dzNl96vBzB06pJpbRwP6RA)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/linkedin/datahub/blob/master/docs/CONTRIBUTING.md)
[![License](https://img.shields.io/github/license/linkedin/datahub)](LICENSE)

---

[Quickstart](docs/quickstart.md) |
[Documentation](#documentation) |
[Features](docs/features.md) |
[Roadmap](docs/roadmap.md) |
[Adoption](#adoption) |
[FAQ](docs/faq.md) |
[Town Hall](docs/townhalls.md)

---

![DataHub](docs/imgs/datahub-logo.png)

> 📣 Next DataHub town hall meeting on Feb 19th, 9am-10am PDT ([convert to your local time](https://greenwichmeantime.com/time/to/pacific-local/))
> - Topic Proposals: [submit here](https://docs.google.com/forms/d/1v2ynbAXjJlqY97xE_X1DAntNrXDznOFiNfryUkMPtkI/)
> - Questions for the team: [add it here](https://docs.google.com/spreadsheets/d/1hCTFQZnhYHAPa-DeIfyye4MlwmrY7GF4hBds5pTZJYM)
> - VC link: https://linkedin.zoom.us/j/4698262890
> - [Meeting details](docs/townhalls.md) & [past recordings](docs/townhall-history.md)

> ✨ Latest Update: 
> - Latest blog post [DataHub: Popular Metadata Architectures Explained](https://engineering.linkedin.com/blog/2020/datahub-popular-metadata-architectures-explained) @ LinkedIn Engineering Blog. 
> - Check out the latest [DataHub Podcast](https://www.dataengineeringpodcast.com/datahub-metadata-management-episode-147/) @ Data Engineering Podcast.
> - We've released v0.6.0. You can find release notes [here](https://github.com/linkedin/datahub/releases/tag/v0.6.0)
> - We're on [Slack](docs/slack.md) now! Ask questions and keep up with the latest announcements.


## Introduction
DataHub is LinkedIn's generalized metadata search & discovery tool. Read about the architectures of different metadata systems and why DataHub excels [here](https://engineering.linkedin.com/blog/2020/datahub-popular-metadata-architectures-explained). Also read our 
[LinkedIn Engineering blog post](https://engineering.linkedin.com/blog/2019/data-hub), check out our [Strata presentation](https://speakerdeck.com/shirshanka/the-evolution-of-metadata-linkedins-journey-strata-nyc-2019) and watch our [Crunch Conference Talk](https://www.youtube.com/watch?v=OB-O0Y6OYDE). You should also visit [DataHub Architecture](docs/architecture/architecture.md) to get a better understanding of how DataHub is implemented and [DataHub Onboarding Guide](docs/how/entity-onboarding.md) to understand how to extend DataHub for your own use cases.


## Quickstart
Please follow the [DataHub Quickstart Guide](docs/quickstart.md) to get a copy of DataHub up & running locally using [Docker](https://docker.com). As the guide assumes some basic knowledge of Docker, we'd recommend you to go through the "Hello World" example of [A Docker Tutorial for Beginners](https://docker-curriculum.com) if Docker is completely foreign to you. 

## Source Code and Repositories
* [linkedin/datahub](https://github.com/linkedin/datahub): This repository contains the complete source code for both DataHub's frontend & backend services. We currently follow a hybrid open source model for development in this repository. See [this blog post](https://engineering.linkedin.com/blog/2020/open-sourcing-datahub--linkedins-metadata-search-and-discovery-p) for details on how we do it. 
* [linkedin/datahub-gma](https://github.com/linkedin/datahub-gma): This repository contains the source code for DataHub's metadata infrastructure libraries (Generalized Metadata Architecture, or GMA). We follow an open-source-first model for development in this repository. 

## Documentation
* [DataHub Developer's Guide](docs/developers.md)
* [DataHub Architecture](docs/architecture/architecture.md)
* [DataHub Onboarding Guide](docs/how/entity-onboarding.md)
* [Docker Images](docker)
* [Frontend](datahub-frontend)
* [Web App](datahub-web)
* [Generalized Metadata Service](gms)
* Metadata Ingestion: \[[python](metadata-ingestion)\] \[[java](metadata-ingestion-examples)\]
* [Metadata Processing Jobs](metadata-jobs)

## Releases
See [Releases](https://github.com/linkedin/datahub/releases) page for more details. We follow the [SemVer Specification](https://semver.org) when versioning the releases and adopt the [Keep a Changelog convention](https://keepachangelog.com/) for the changelog format.

## FAQs
Frequently Asked Questions about DataHub can be found [here](docs/faq.md).

## Features & Roadmap
Check out DataHub's [Features](docs/features.md) & [Roadmap](docs/roadmap.md).

## Contributing
We welcome contributions from the community. Please refer to our [Contributing Guidelines](docs/CONTRIBUTING.md) for more details. We also have a [contrib](contrib) directory for incubating experimental features.

## Community
Join our [slack workspace](https://join.slack.com/t/datahubspace/shared_invite/zt-dkzbxfck-dzNl96vBzB06pJpbRwP6RA) for discussions and important announcements. You can also find out more about our upcoming [town hall meetings](docs/townhalls.md) and view past recordings.

## Adoption
Here are the companies that have officially adopted DataHub. Please feel free to add yours to the list if we missed it.
* [Expedia Group](http://expedia.com)
* [Experius](https://www.experius.nl)
* [Grofers](https://grofers.com)
* [LinkedIn](http://linkedin.com)
* [Saxo Bank](https://www.home.saxo)
* [Shanghai HuaRui Bank](https://www.shrbank.com)
* [TypeForm](http://typeform.com)
* [Valassis]( https://www.valassis.com)

Here is a list of companies that are currently building POC or seriously evaluating DataHub.
* [Amadeus](https://www.amadeus.com)
* [Bizzy Group](https://www.bizzy.co.id)
* [Booking.com](https://www.booking.com)
* [Experian](https://www.experian.com)
* [FlixBus](https://www.flixbus.com)
* [Geotab](https://www.geotab.com)
* [Kindred Group](https://www.kindredgroup.com)
* [Instructure](https://www.instructure.com)
* [Inventec](https://www.inventec.com)
* [Microsoft](https://microsoft.com)
* [Morgan Stanley](https://www.morganstanley.com)
* [Orange Telecom](https://www.orange.com)
* [REEF Technology](https://reeftechnology.com)
* [SpotHero](https://spothero.com)
* [Sysco AS](https://sysco.no)
* [ThoughtWorks](https://www.thoughtworks.com)
* [University of Phoenix](https://www.phoenix.edu)
* [Vectice](https://www.vectice.com)
* [Viasat](https://viasat.com)
* [Wolt](https://wolt.com)
* [Weee!](https://www.sayweee.com)

## Select Articles & Talks
* [DataHub: A Generalized Metadata Search & Discovery Tool](https://engineering.linkedin.com/blog/2019/data-hub)
* [DataHub: Popular Metadata Architectures Explained](https://engineering.linkedin.com/blog/2020/datahub-popular-metadata-architectures-explained)
* [Open sourcing DataHub: LinkedIn’s metadata search and discovery platform](https://engineering.linkedin.com/blog/2020/open-sourcing-datahub--linkedins-metadata-search-and-discovery-p)
* [DataHub: Powering LinkedIn's Metadata](https://github.com/linkedin/datahub/blob/master/docs/demo/DataHub%20-%20Powering%20LinkedIn%E2%80%99s%20Metadata.pdf) @ [Budapest Data Forum 2020](https://budapestdata.hu/2020/en/)
* [Taming the Data Beast Using DataHub](https://www.youtube.com/watch?v=bo4OhiPro7Y) @ [Data Engineering Melbourne Meetup November 2020](https://www.meetup.com/Data-Engineering-Melbourne/events/kgnvlrybcpbjc/)
* [Metadata Management And Integration At LinkedIn With DataHub](https://www.dataengineeringpodcast.com/datahub-metadata-management-episode-147/) @ [Data Engineering Podcast](https://www.dataengineeringpodcast.com)
* [The evolution of metadata: LinkedIn’s story](https://speakerdeck.com/shirshanka/the-evolution-of-metadata-linkedins-journey-strata-nyc-2019) @ [Strata Data Conference 2019](https://conferences.oreilly.com/strata/strata-ny-2019.html)
* [Journey of metadata at LinkedIn](https://www.youtube.com/watch?v=OB-O0Y6OYDE) @ [Crunch Data Conference 2019](https://crunchconf.com/2019)
* [DataHub Journey with Expedia Group](https://www.youtube.com/watch?v=ajcRdB22s5o)
* [Saxo Bank's Data Workbench](https://www.slideshare.net/SheetalPratik/linkedinsaxobankdataworkbench)
* [Data Discoverability at SpotHero](https://www.slideshare.net/MaggieHays/data-discoverability-at-spothero)
* [Data Catalogue — Knowing your data](https://medium.com/albert-franzi/data-catalogue-knowing-your-data-15f7d0724900)
* [LinkedIn DataHub Application Architecture Quick Understanding](https://medium.com/@liangjunjiang/linkedin-datahub-application-architecture-quick-understanding-a5b7868ee205)
* [LinkIn Datahub Metadata Ingestion Scripts Unofficical Guide](https://medium.com/@liangjunjiang/linkin-datahub-etl-unofficical-guide-7c3949483f8b)
* [A Dive Into Metadata Hubs](https://www.holistics.io/blog/a-dive-into-metadata-hubs/)
* [25 Hot New Data Tools and What They DON’T Do](https://blog.amplifypartners.com/25-hot-new-data-tools-and-what-they-dont-do/)
* [Emerging Architectures for Modern Data Infrastructure](https://a16z.com/2020/10/15/the-emerging-architectures-for-modern-data-infrastructure/)

See the full list [here](docs/links.md).
