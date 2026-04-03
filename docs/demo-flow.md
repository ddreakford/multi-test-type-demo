# Demo Flow (suggested)
1) **Clone repo**
    - `git clone --recurse-submodules https://github.com/ddreakford/multi-test-type-demo.git`
    - `cd multi-test-type-demo`
2) **Start AUT** 
    - `cd restful-booker-platform	# included as a submodule` 
    - `DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose up –d docker compose up`
3) **Run the automated suite** 
    - `cd rbp-test-demo`
    - `./gradlew clean test`
4) **Run the RCA suite** 
    - `cd rbp-test-demo`
    - `gradlew rcaDemo      # intentional failures`
5) **View Allure Report** 
    - `./gradlew allureServe`
    - Dashboards, test detail, screenshots
    - RCA walkthrough
6) **Manual test tutorial** 
    - Reference screenshots alongside automated equivalents