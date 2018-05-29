const grpc=require('grpc');
const PROTO_PATH="../src/main/proto/db.proto";
const db=grpc.load(PROTO_PATH);
const config=require('../config.json');
const bluebird=require('bluebird');
const assert=require('assert');
bluebird.promisifyAll(db.blockdb.BlockDatabase.prototype);
const stub=new db.blockdb.BlockDatabase(config["1"]["ip"]+":"+config["1"]["port"], grpc.credentials.createInsecure());
//Function to start a server that does not crash.
async function startStable(){


}
//Function to start a server that often crashes.
async function startUnstable(){

}
const client={
	"LogLength": async ()=>{
		return (await stub.logLengthAsync({})).Value;
	},
	"GET": async(user)=>{
		return (await stub.getAsync({"UserID": user})).Value;
	},
	"PUT": async(user, val)=>{
		return (await stub.putAsync({"UserID": user, "Value": val})).Success;
	},
	"DEPOSIT": async(user, val)=>{
		return (await stub.depositAsync({"UserID": user, "Value": val})).Success;
	},
	"WITHDRAW": async(user, val)=>{
		return (await stub.withdrawAsync({"UserID": user, "Value": val})).Success;
		
	},
	"TRANSFER": async(ufrom, uto, val)=>{
		return (await stub.transferAsync({"FromID": ufrom, "ToID": uto, "Value": val})).Success;
	}
	
	
}

async function test(){
	console.log("Step 1: Initialize account")
	for(let I=0;I<=9;I++){
		assert(await client.PUT(`TEST---${I}`,10));
	}
	console.log("Check value: expecting value=10")
	assert(10==await client.GET("TEST---5"));

	console.log("Check LogLength: expecting value=10")
	assert(10==await client.LogLength());

	console.log("Step 2: Try deposit")
	for(let I=0;I<=9;I++){
		assert(await client.DEPOSIT(`TEST---${I}`,5))
	}
	console.log("Check value: expecting value=15")
	assert(15==await client.GET("TEST---5"));

	console.log("Step 3: Try transfer")
	for(let I=0;I<=9;I++){
		assert(await client.TRANSFER(`TEST---${I}`,"TEST--TX" ,10));
	}
	console.log("Check value: expecting value=100")
	assert(100==await client.GET("TEST--TX"));

	console.log("Step 4: Try transfer again")
	for(let I=0;I<=9;I++){
		assert(await client.TRANSFER("TEST--TX" ,`TEST---${I}` ,5));
	}
	console.log("Check value: expecting value=50")
	assert(50==await client.GET("TEST--TX"));

	console.log("Step 5: Try withdraw")
	for(let I=0;I<=9;I++){
		assert(await client.WITHDRAW(`TEST---${I}` ,5));
	}
	console.log("Check value: expecting value=5");
        assert(5==await client.GET("TEST---2"));

	console.log("Step 5: Try overdraft")
	for(let I=0;I<=9;I++){
		assert(!await client.WITHDRAW(`TEST---${I}` ,10));
	}
	console.log("Check value: expecting value=5")
	assert(5==await client.GET("TEST---2"));

	console.log("Check LogLength: expecting value<=50")
	assert(50>=await client.LogLength())

	//console.log "Try Killing the server and restart..."
	//console.log "Sleep for a while, waiting for hashmap reconstruction..."
	//sleep 10;

	console.log("Check value again: expecting value=5")
	assert(5==await client.GET("TEST---2"));


	console.log("Test completed. Please verify JSON block output with example_1.json .")
	
}
test().then(console.log).catch(console.log);
