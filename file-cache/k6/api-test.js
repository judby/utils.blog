// import necessary modules
import { check } from 'k6';
import http from 'k6/http';

// define configuration
export const options = {
    // define thresholds
    thresholds: {
        http_req_failed: [{ threshold: 'rate<0.01' }], // http errors should be less than 1%
        http_req_duration: ['p(99)<1000'], // 99% of requests should be below 1s
    },
    scenarios: {
        breaking: {
            executor: 'ramping-vus',
            stages: [
                { duration: '10s', target: 20 },
                { duration: '50s', target: 20 },
                { duration: '50s', target: 40 },
                { duration: '50s', target: 60 },
                { duration: '50s', target: 80 },
                { duration: '50s', target: 100 },
                { duration: '50s', target: 120 },
                { duration: '50s', target: 140 },
                //....
            ],
        },
    },
};

export default function () {
    // define URL
    const url = 'http://localhost:8080/api/images/numbers/' + Math.floor(Math.random() * 10000);

    // send a GET request and save response as a variable
    const res = http.get(url);

    // check that response is 200
    check(res, {
        'response code was 200': (res) => res.status == 200,
    });
}
