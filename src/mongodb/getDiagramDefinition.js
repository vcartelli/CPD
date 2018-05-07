db.models.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			  "_id": "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3", // "{diagramId}"
			}
			
			/*
			 *                           model         parent
			 * TODO: diagram -> plane -<                 ^
			 *                           depictions -> model
			 *                                           v
			 *                                         childs
			 */
		},

		// Stage 2
		{
			$project: {
			    "diagram": "$$ROOT"
			}
		},

		// Stage 3
		{
			$lookup: {
			    from: "diagrams",
			    localField: "diagram.planeId",
			    foreignField: "_id",
			    as: "plane"
			}
		},

		// Stage 4
		{
			$unwind: "$plane"
		},

		// Stage 5
		{
			$lookup: {
			    from: "diagrams",
			    localField: "plane._id",
			    foreignField: "planeId",
			    as: "elements"
			}
		},

		// Stage 6
		{
			$lookup: {
			    from: "models",
			    localField: "plane.modelId",
			    foreignField: "_id",
			    as: "root"
			}
		},

		// Stage 7
		{
			$unwind: "$root"
		},

		// Stage 8
		{
			$graphLookup: {
			    from: "models",
			    startWith: "$root._id",
			    connectFromField: "_id",
			    connectToField: "parentId",
			    as: "childs"
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
