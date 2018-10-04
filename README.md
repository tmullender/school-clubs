# School Clubs Allocator

This will be an app that reads a CSV file in the format: 

```Timestamp, Name, Class, 1st Choice, 2nd Choice, 3rd Choice, 1st Choice, 2nd Choice, 3rd Choice```

and attempts to allocate the most people to each club with the following rules:

* Priority is given to oldest pupils first, based on class
* There is a maximum number of pupils for each club
* Some clubs can not be attended in both terms
* A pupil can only attend one club on any given day
* If there are multiple requests from a pupil the latest request is considered  

For the original scripts please see the scripts branch
